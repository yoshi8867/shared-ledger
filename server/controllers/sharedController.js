const { pool } = require('../db/pool');

// GET /api/shared-ledgers?ledger_id= — 특정 장부의 공유 사용자 목록 (소유자만)
async function getSharedUsers(req, res) {
  const { ledger_id } = req.query;
  const userId = req.user.user_id;

  if (!ledger_id) return res.status(400).json({ error: 'ledger_id 파라미터가 필요합니다' });

  try {
    const ownerCheck = await pool.query(
      `SELECT 1 FROM ledgers WHERE ledger_id = $1 AND owner_id = $2`,
      [ledger_id, userId]
    );
    if (ownerCheck.rows.length === 0) {
      return res.status(403).json({ error: '장부 소유자만 공유 목록을 조회할 수 있습니다' });
    }

    const result = await pool.query(
      `SELECT sl.shared_ledger_id, sl.permission, sl.created_at,
              u.user_id, u.email, u.name
       FROM shared_ledgers sl
       JOIN users u ON u.user_id = sl.shared_with_user_id
       WHERE sl.ledger_id = $1
       ORDER BY sl.created_at ASC`,
      [ledger_id]
    );
    res.json({ shared_users: result.rows });
  } catch (err) {
    console.error('[getSharedUsers]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// POST /api/shared-ledgers — 이메일로 사용자 초대
async function invite(req, res) {
  const { ledger_id, email, permission = 'view' } = req.body;
  const ownerId = req.user.user_id;

  if (!ledger_id || !email) {
    return res.status(400).json({ error: 'ledger_id, email은 필수입니다' });
  }
  if (!['view', 'edit'].includes(permission)) {
    return res.status(400).json({ error: "permission은 'view' 또는 'edit'이어야 합니다" });
  }

  try {
    const ownerCheck = await pool.query(
      `SELECT 1 FROM ledgers WHERE ledger_id = $1 AND owner_id = $2`,
      [ledger_id, ownerId]
    );
    if (ownerCheck.rows.length === 0) {
      return res.status(403).json({ error: '장부 소유자만 초대할 수 있습니다' });
    }

    const userResult = await pool.query(
      `SELECT user_id FROM users WHERE email = $1`,
      [email]
    );
    if (userResult.rows.length === 0) {
      return res.status(404).json({ error: '해당 이메일의 사용자를 찾을 수 없습니다' });
    }
    const targetUserId = userResult.rows[0].user_id;

    if (Number(targetUserId) === Number(ownerId)) {
      return res.status(400).json({ error: '본인을 초대할 수 없습니다' });
    }

    const result = await pool.query(
      `INSERT INTO shared_ledgers (ledger_id, shared_with_user_id, permission)
       VALUES ($1, $2, $3)
       ON CONFLICT (ledger_id, shared_with_user_id)
         DO UPDATE SET permission = EXCLUDED.permission
       RETURNING *`,
      [ledger_id, targetUserId, permission]
    );
    res.status(201).json({ shared: result.rows[0] });
  } catch (err) {
    console.error('[invite]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// PATCH /api/shared-ledgers/:id — 권한 변경 (소유자만)
async function updatePermission(req, res) {
  const { id } = req.params;
  const { permission } = req.body;
  const userId = req.user.user_id;

  if (!['view', 'edit'].includes(permission)) {
    return res.status(400).json({ error: "permission은 'view' 또는 'edit'이어야 합니다" });
  }

  try {
    const check = await pool.query(
      `SELECT sl.shared_ledger_id FROM shared_ledgers sl
       JOIN ledgers l ON l.ledger_id = sl.ledger_id
       WHERE sl.shared_ledger_id = $1 AND l.owner_id = $2`,
      [id, userId]
    );
    if (check.rows.length === 0) {
      return res.status(403).json({ error: '권한을 변경할 수 없습니다' });
    }

    const result = await pool.query(
      `UPDATE shared_ledgers SET permission = $1 WHERE shared_ledger_id = $2 RETURNING *`,
      [permission, id]
    );
    res.json({ shared: result.rows[0] });
  } catch (err) {
    console.error('[updatePermission]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// DELETE /api/shared-ledgers/:id — 공유 해제 (소유자 or 본인 공유)
async function revokeAccess(req, res) {
  const { id } = req.params;
  const userId = req.user.user_id;

  try {
    const check = await pool.query(
      `SELECT sl.shared_ledger_id FROM shared_ledgers sl
       JOIN ledgers l ON l.ledger_id = sl.ledger_id
       WHERE sl.shared_ledger_id = $1
         AND (l.owner_id = $2 OR sl.shared_with_user_id = $2)`,
      [id, userId]
    );
    if (check.rows.length === 0) {
      return res.status(403).json({ error: '공유를 해제할 권한이 없습니다' });
    }

    await pool.query(`DELETE FROM shared_ledgers WHERE shared_ledger_id = $1`, [id]);
    res.status(204).send();
  } catch (err) {
    console.error('[revokeAccess]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// GET /api/ledgers/shared — 나와 공유된 장부 목록
async function getSharedLedgers(req, res) {
  const userId = req.user.user_id;

  try {
    const result = await pool.query(
      `SELECT l.ledger_id, l.ledger_name, l.owner_id,
              u.name AS owner_name, u.email AS owner_email,
              sl.permission, sl.shared_ledger_id
       FROM shared_ledgers sl
       JOIN ledgers l ON l.ledger_id = sl.ledger_id
       JOIN users u ON u.user_id = l.owner_id
       WHERE sl.shared_with_user_id = $1
       ORDER BY sl.created_at ASC`,
      [userId]
    );
    res.json({ ledgers: result.rows });
  } catch (err) {
    console.error('[getSharedLedgers]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { getSharedUsers, invite, updatePermission, revokeAccess, getSharedLedgers };
