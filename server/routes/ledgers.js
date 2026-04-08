const express = require('express');
const router = express.Router();
const { pool } = require('../db/pool');
const authMiddleware = require('../middleware/auth');
const { checkRead, checkEdit } = require('../middleware/ledgerAccess');
const { getTransactions, createTransaction } = require('../controllers/transactionController');
const { getCategories, createCategory } = require('../controllers/categoryController');

// GET /api/ledgers — 내 장부 목록
router.get('/', authMiddleware, async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT * FROM ledgers WHERE owner_id = $1 ORDER BY created_at ASC`,
      [req.user.user_id]
    );
    res.json({ ledgers: result.rows });
  } catch (err) {
    console.error('[getLedgers]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /api/ledgers — 장부 생성
router.post('/', authMiddleware, async (req, res) => {
  const { ledger_name } = req.body;
  try {
    const result = await pool.query(
      `INSERT INTO ledgers (owner_id, ledger_name) VALUES ($1, $2) RETURNING *`,
      [req.user.user_id, ledger_name || '내 장부']
    );
    res.status(201).json({ ledger: result.rows[0] });
  } catch (err) {
    console.error('[createLedger]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /api/ledgers/shared — 나와 공유된 장부 목록
const { getSharedLedgers } = require('../controllers/sharedController');
router.get('/shared', authMiddleware, getSharedLedgers);

// POST /api/ledgers/join — 초대 코드로 장부 참가
router.post('/join', authMiddleware, async (req, res) => {
  const { invite_code } = req.body;
  const userId = req.user.user_id;

  if (!invite_code) return res.status(400).json({ error: 'invite_code가 필요합니다' });

  try {
    const result = await pool.query(
      `SELECT ledger_id, owner_id, invite_expires_at FROM ledgers WHERE invite_code = $1`,
      [invite_code.toUpperCase()]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: '유효하지 않은 초대 코드입니다' });
    }

    const ledger = result.rows[0];

    if (new Date() > new Date(ledger.invite_expires_at)) {
      return res.status(410).json({ error: '만료된 초대 코드입니다' });
    }
    if (Number(ledger.owner_id) === Number(userId)) {
      return res.status(400).json({ error: '본인의 장부에는 참가할 수 없습니다' });
    }

    await pool.query(
      `INSERT INTO shared_ledgers (ledger_id, shared_with_user_id, permission)
       VALUES ($1, $2, 'view')
       ON CONFLICT (ledger_id, shared_with_user_id) DO NOTHING`,
      [ledger.ledger_id, userId]
    );

    res.json({ message: '장부에 참가했습니다', ledger_id: ledger.ledger_id });
  } catch (err) {
    console.error('[joinLedger]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /api/ledgers/:id — 장부 단건 조회
router.get('/:id', authMiddleware, checkRead, async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT * FROM ledgers WHERE ledger_id = $1`,
      [req.params.id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: '장부를 찾을 수 없습니다' });
    }
    res.json({ ledger: result.rows[0] });
  } catch (err) {
    console.error('[getLedger]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /api/ledgers/:id/invite-code — 초대 코드 생성/갱신 (소유자만)
router.post('/:id/invite-code', authMiddleware, async (req, res) => {
  const { id } = req.params;
  const userId = req.user.user_id;

  try {
    const ownerCheck = await pool.query(
      `SELECT 1 FROM ledgers WHERE ledger_id = $1 AND owner_id = $2`,
      [id, userId]
    );
    if (ownerCheck.rows.length === 0) {
      return res.status(403).json({ error: '장부 소유자만 초대 코드를 생성할 수 있습니다' });
    }

    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let code = '';
    for (let i = 0; i < 6; i++) {
      code += chars[Math.floor(Math.random() * chars.length)];
    }
    const expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000); // 7일

    await pool.query(
      `UPDATE ledgers SET invite_code = $1, invite_expires_at = $2 WHERE ledger_id = $3`,
      [code, expiresAt, id]
    );

    res.json({ invite_code: code, expires_at: expiresAt });
  } catch (err) {
    console.error('[generateInviteCode]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// PUT /api/ledgers/:id — 장부 수정
router.put('/:id', authMiddleware, checkEdit, async (req, res) => {
  const { ledger_name } = req.body;
  try {
    const result = await pool.query(
      `UPDATE ledgers SET ledger_name = COALESCE($1, ledger_name), updated_at = NOW()
       WHERE ledger_id = $2 RETURNING *`,
      [ledger_name || null, req.params.id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: '장부를 찾을 수 없습니다' });
    }
    res.json({ ledger: result.rows[0] });
  } catch (err) {
    console.error('[updateLedger]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// 거래 중첩 라우트
router.get('/:ledgerId/transactions', authMiddleware, checkRead, getTransactions);
router.post('/:ledgerId/transactions', authMiddleware, checkEdit, createTransaction);

// 카테고리 중첩 라우트
router.get('/:ledgerId/categories', authMiddleware, checkRead, getCategories);
router.post('/:ledgerId/categories', authMiddleware, checkEdit, createCategory);

module.exports = router;
