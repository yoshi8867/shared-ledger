const { pool } = require('../db/pool');

// 조회 권한: 소유자 OR 공유(view/edit 모두)
async function checkRead(req, res, next) {
  const ledgerId = req.params.ledgerId || req.params.id;
  const userId = req.user.user_id;

  try {
    const result = await pool.query(
      `SELECT 1 FROM ledgers WHERE ledger_id = $1 AND owner_id = $2
       UNION
       SELECT 1 FROM shared_ledgers WHERE ledger_id = $1 AND shared_with_user_id = $2`,
      [ledgerId, userId]
    );
    if (result.rows.length === 0) {
      return res.status(403).json({ error: '이 장부에 접근할 권한이 없습니다' });
    }
    next();
  } catch (err) {
    console.error('[checkRead]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// 편집 권한: 소유자 OR 공유(edit만)
async function checkEdit(req, res, next) {
  const ledgerId = req.params.ledgerId || req.params.id;
  const userId = req.user.user_id;

  try {
    const result = await pool.query(
      `SELECT 1 FROM ledgers WHERE ledger_id = $1 AND owner_id = $2
       UNION
       SELECT 1 FROM shared_ledgers WHERE ledger_id = $1 AND shared_with_user_id = $2 AND permission = 'edit'`,
      [ledgerId, userId]
    );
    if (result.rows.length === 0) {
      return res.status(403).json({ error: '이 장부를 수정할 권한이 없습니다' });
    }
    next();
  } catch (err) {
    console.error('[checkEdit]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { checkRead, checkEdit };
