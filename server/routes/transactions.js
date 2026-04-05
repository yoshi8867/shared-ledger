const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth');

// S5에서 구현 예정

router.get('/', authMiddleware, (req, res) => {
  // GET /api/transactions?ledger_id=1&month=2026-04
  res.status(501).json({ error: 'Not implemented (S5)' });
});

router.post('/', authMiddleware, (req, res) => {
  res.status(501).json({ error: 'Not implemented (S5)' });
});

router.post('/batch', authMiddleware, (req, res) => {
  res.status(501).json({ error: 'Not implemented (S5)' });
});

router.get('/:id', authMiddleware, (req, res) => {
  res.status(501).json({ error: 'Not implemented (S5)' });
});

router.put('/:id', authMiddleware, (req, res) => {
  res.status(501).json({ error: 'Not implemented (S5)' });
});

router.delete('/:id', authMiddleware, (req, res) => {
  res.status(501).json({ error: 'Not implemented (S5)' });
});

module.exports = router;
