const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const https = require('https');
const { OAuth2Client } = require('google-auth-library');
const { pool } = require('../db/pool');

const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

const SALT_ROUNDS = 10;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function signAccessToken(user) {
  return jwt.sign(
    { user_id: user.user_id, email: user.email },
    process.env.JWT_SECRET,
    { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
  );
}

function signRefreshToken(user) {
  return jwt.sign(
    { user_id: user.user_id, type: 'refresh' },
    process.env.JWT_SECRET,
    { expiresIn: process.env.JWT_REFRESH_EXPIRES_IN || '30d' }
  );
}

// POST /api/auth/signup
async function signup(req, res) {
  const { email, password, name } = req.body;

  if (!email || !password || !name) {
    return res.status(400).json({ error: 'email, password, name은 필수입니다' });
  }
  if (!EMAIL_REGEX.test(email)) {
    return res.status(400).json({ error: '이메일 형식이 올바르지 않습니다' });
  }
  if (password.length < 6) {
    return res.status(400).json({ error: '비밀번호는 최소 6자 이상이어야 합니다' });
  }

  try {
    const existing = await pool.query('SELECT user_id FROM users WHERE email = $1', [email]);
    if (existing.rows.length > 0) {
      return res.status(409).json({ error: '이미 등록된 이메일입니다' });
    }

    const password_hash = await bcrypt.hash(password, SALT_ROUNDS);

    const userResult = await pool.query(
      `INSERT INTO users (email, password_hash, name, auth_provider)
       VALUES ($1, $2, $3, 'email')
       RETURNING user_id, email, name, auth_provider, created_at`,
      [email, password_hash, name]
    );
    const user = userResult.rows[0];

    // 기본 장부 자동 생성
    const ledgerResult = await pool.query(
      `INSERT INTO ledgers (owner_id, ledger_name) VALUES ($1, '내 장부')
       RETURNING ledger_id`,
      [user.user_id]
    );
    const ledger_id = ledgerResult.rows[0].ledger_id;

    const access_token = signAccessToken(user);
    const refresh_token = signRefreshToken(user);

    res.status(201).json({
      access_token,
      refresh_token,
      ledger_id,
      user: {
        user_id: user.user_id,
        email: user.email,
        name: user.name,
        auth_provider: user.auth_provider,
      },
    });
  } catch (err) {
    console.error('[signup]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// POST /api/auth/login
async function login(req, res) {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ error: 'email과 password는 필수입니다' });
  }

  try {
    const result = await pool.query(
      'SELECT user_id, email, name, password_hash, auth_provider FROM users WHERE email = $1',
      [email]
    );

    if (result.rows.length === 0) {
      return res.status(401).json({ error: '이메일 또는 비밀번호가 올바르지 않습니다' });
    }

    const user = result.rows[0];

    if (!user.password_hash) {
      return res.status(401).json({ error: '이 계정은 소셜 로그인을 사용합니다. Google 또는 Naver로 로그인해 주세요.' });
    }

    const valid = await bcrypt.compare(password, user.password_hash);
    if (!valid) {
      return res.status(401).json({ error: '이메일 또는 비밀번호가 올바르지 않습니다' });
    }

    const access_token = signAccessToken(user);
    const refresh_token = signRefreshToken(user);

    // 주 장부 ID 조회
    const ledgerResult = await pool.query(
      'SELECT ledger_id FROM ledgers WHERE owner_id = $1 ORDER BY created_at ASC LIMIT 1',
      [user.user_id]
    );
    const ledger_id = ledgerResult.rows[0]?.ledger_id ?? null;

    res.json({
      access_token,
      refresh_token,
      ledger_id,
      user: {
        user_id: user.user_id,
        email: user.email,
        name: user.name,
        auth_provider: user.auth_provider,
      },
    });
  } catch (err) {
    console.error('[login]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// POST /api/auth/refresh
async function refresh(req, res) {
  const { refresh_token } = req.body;

  if (!refresh_token) {
    return res.status(400).json({ error: 'refresh_token은 필수입니다' });
  }

  try {
    const decoded = jwt.verify(refresh_token, process.env.JWT_SECRET);

    if (decoded.type !== 'refresh') {
      return res.status(401).json({ error: '유효하지 않은 refresh token입니다' });
    }

    const result = await pool.query(
      'SELECT user_id, email FROM users WHERE user_id = $1',
      [decoded.user_id]
    );
    if (result.rows.length === 0) {
      return res.status(401).json({ error: '사용자를 찾을 수 없습니다' });
    }

    res.json({ access_token: signAccessToken(result.rows[0]) });
  } catch (err) {
    if (err.name === 'JsonWebTokenError' || err.name === 'TokenExpiredError') {
      return res.status(401).json({ error: 'refresh token이 만료되었거나 유효하지 않습니다' });
    }
    console.error('[refresh]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// GET /api/auth/verify  (authMiddleware가 먼저 검증)
async function verify(req, res) {
  try {
    const result = await pool.query(
      'SELECT user_id, email, name, auth_provider FROM users WHERE user_id = $1',
      [req.user.user_id]
    );
    if (result.rows.length === 0) {
      return res.status(401).json({ error: '사용자를 찾을 수 없습니다' });
    }
    res.json({ user: result.rows[0] });
  } catch (err) {
    console.error('[verify]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// POST /api/auth/oauth/google
async function loginWithGoogle(req, res) {
  const { id_token } = req.body;
  if (!id_token) {
    return res.status(400).json({ error: 'id_token은 필수입니다' });
  }

  try {
    const ticket = await googleClient.verifyIdToken({
      idToken: id_token,
      audience: process.env.GOOGLE_CLIENT_ID,
    });
    const payload = ticket.getPayload();
    const { email, name } = payload;

    // upsert: 없으면 생성, 있으면 조회
    const upsertResult = await pool.query(
      `INSERT INTO users (email, name, auth_provider)
       VALUES ($1, $2, 'google')
       ON CONFLICT (email) DO UPDATE
         SET auth_provider = 'google'
       RETURNING user_id, email, name, auth_provider`,
      [email, name]
    );
    const user = upsertResult.rows[0];

    // 장부가 없으면 기본 장부 생성
    const ledgerCheck = await pool.query(
      'SELECT ledger_id FROM ledgers WHERE owner_id = $1 ORDER BY created_at ASC LIMIT 1',
      [user.user_id]
    );
    let ledger_id;
    if (ledgerCheck.rows.length === 0) {
      const ledgerResult = await pool.query(
        `INSERT INTO ledgers (owner_id, ledger_name) VALUES ($1, '내 장부') RETURNING ledger_id`,
        [user.user_id]
      );
      ledger_id = ledgerResult.rows[0].ledger_id;
    } else {
      ledger_id = ledgerCheck.rows[0].ledger_id;
    }

    const access_token = signAccessToken(user);
    const refresh_token = signRefreshToken(user);

    res.json({ access_token, refresh_token, ledger_id });
  } catch (err) {
    console.error('[loginWithGoogle]', err);
    res.status(401).json({ error: 'Google 인증에 실패했습니다' });
  }
}

// 네이버 프로필 API 호출
function getNaverProfile(naverAccessToken) {
  return new Promise((resolve, reject) => {
    const req = https.request(
      {
        hostname: 'openapi.naver.com',
        path: '/v1/nid/me',
        method: 'GET',
        headers: { Authorization: `Bearer ${naverAccessToken}` },
      },
      (res) => {
        let data = '';
        res.on('data', (chunk) => (data += chunk));
        res.on('end', () => {
          try { resolve(JSON.parse(data)); } catch (e) { reject(e); }
        });
      }
    );
    req.on('error', reject);
    req.end();
  });
}

// POST /api/auth/oauth/naver
async function loginWithNaver(req, res) {
  const { access_token: naverToken } = req.body;
  if (!naverToken) {
    return res.status(400).json({ error: 'access_token은 필수입니다' });
  }

  try {
    const profile = await getNaverProfile(naverToken);
    if (profile.resultcode !== '00') {
      return res.status(401).json({ error: '네이버 인증에 실패했습니다' });
    }

    const { email, name } = profile.response;
    if (!email) {
      return res.status(400).json({ error: '네이버 계정에 이메일이 없습니다. 이메일 제공 동의가 필요합니다.' });
    }

    const upsertResult = await pool.query(
      `INSERT INTO users (email, name, auth_provider)
       VALUES ($1, $2, 'naver')
       ON CONFLICT (email) DO UPDATE
         SET auth_provider = 'naver'
       RETURNING user_id, email, name, auth_provider`,
      [email, name || '네이버 사용자']
    );
    const user = upsertResult.rows[0];

    const ledgerCheck = await pool.query(
      'SELECT ledger_id FROM ledgers WHERE owner_id = $1 ORDER BY created_at ASC LIMIT 1',
      [user.user_id]
    );
    let ledger_id;
    if (ledgerCheck.rows.length === 0) {
      const ledgerResult = await pool.query(
        `INSERT INTO ledgers (owner_id, ledger_name) VALUES ($1, '내 장부') RETURNING ledger_id`,
        [user.user_id]
      );
      ledger_id = ledgerResult.rows[0].ledger_id;
    } else {
      ledger_id = ledgerCheck.rows[0].ledger_id;
    }

    const access_token = signAccessToken(user);
    const refresh_token = signRefreshToken(user);

    res.json({ access_token, refresh_token, ledger_id });
  } catch (err) {
    console.error('[loginWithNaver]', err);
    res.status(401).json({ error: '네이버 인증에 실패했습니다' });
  }
}

module.exports = { signup, login, refresh, verify, loginWithGoogle, loginWithNaver };
