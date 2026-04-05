const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth');

// S5에서 구현 예정

router.get('/', authMiddleware, (req, res) => {
  res.status(501).json({ error: 'Not implemented (S5)' });
});

router.post('/', authMiddleware, (req, res) => {
  res.status(501).json({ error: 'Not implemented (S5)' });
});

router.get('/shared', authMiddleware, (req, res) => {
  // 나와 공유된 장부 목록 — S7에서 구현
  res.status(501).json({ error: 'Not implemented (S7)' });
});

router.get('/:id', authMiddleware, (req, res) => {
  res.status(501).json({ error: 'Not implemented (S5)' });
});

router.put('/:id', authMiddleware, (req, res) => {
  res.status(501).json({ error: 'Not implemented (S5)' });
});

module.exports = router;
