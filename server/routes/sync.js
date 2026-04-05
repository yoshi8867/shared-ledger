const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth');

// S6에서 구현 예정

router.get('/delta', authMiddleware, (req, res) => {
  // GET /api/sync/delta?ledger_id=1&since=2026-04-01T00:00:00Z
  res.status(501).json({ error: 'Not implemented (S6)' });
});

router.post('/push', authMiddleware, (req, res) => {
  res.status(501).json({ error: 'Not implemented (S6)' });
});

module.exports = router;
