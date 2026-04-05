const express = require('express');
const router = express.Router();
const { pool } = require('../db/pool');

router.get('/health', async (req, res) => {
  let dbStatus = 'not configured';
  if (pool) {
    try {
      await pool.query('SELECT 1');
      dbStatus = 'connected';
    } catch {
      dbStatus = 'disconnected';
    }
  }

  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    db: dbStatus,
  });
});

module.exports = router;
