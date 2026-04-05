const express = require('express');
const router = express.Router();
const { pool } = require('../db/pool');

// GET /api/settings/login-required
// 앱 시작 시 로그인 필수 여부를 서버에서 동적으로 제어
router.get('/login-required', async (req, res) => {
  if (!pool) {
    return res.json({ login_required: false });
  }
  try {
    const result = await pool.query(
      "SELECT setting_value FROM system_settings WHERE setting_key = 'login_required'"
    );
    const value = result.rows[0]?.setting_value ?? 'false';
    res.json({ login_required: value === 'true' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'DB error' });
  }
});

module.exports = router;
