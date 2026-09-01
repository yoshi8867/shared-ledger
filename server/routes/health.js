const express = require('express');
const router = express.Router();
const { pool } = require('../db/pool');

// ponytail: keep-alive 핑 전용 — DB를 절대 건드리지 않는다.
// (핑이 DB를 깨우면 Neon 자동정지가 무력화돼 무료 컴퓨트 시간이 소진됨)
router.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// DB 상태를 실제로 보고 싶을 때만 수동으로 호출 (핑 대상 아님)
router.get('/health/db', async (req, res) => {
  let db = 'not configured';
  if (pool) {
    try {
      await pool.query('SELECT 1');
      db = 'connected';
    } catch {
      db = 'disconnected';
    }
  }
  res.json({ status: 'ok', db });
});

module.exports = router;
