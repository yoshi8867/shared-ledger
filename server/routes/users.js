const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth');
const { pool } = require('../db/pool');

// GET /api/users/search?email= — 이메일로 사용자 검색
router.get('/search', authMiddleware, async (req, res) => {
  const { email } = req.query;
  if (!email) return res.status(400).json({ error: 'email 파라미터가 필요합니다' });

  try {
    const result = await pool.query(
      `SELECT user_id, email, name FROM users WHERE email = $1`,
      [email]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: '사용자를 찾을 수 없습니다' });
    }
    res.json({ user: result.rows[0] });
  } catch (err) {
    console.error('[userSearch]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
