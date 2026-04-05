const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth');

// S7에서 구현 예정

router.post('/', authMiddleware, (req, res) => {
  // 장부 공유 초대
  res.status(501).json({ error: 'Not implemented (S7)' });
});

router.get('/', authMiddleware, (req, res) => {
  // GET /api/shared-ledgers?ledger_id=1 — 공유 사용자 목록
  res.status(501).json({ error: 'Not implemented (S7)' });
});

router.put('/:id', authMiddleware, (req, res) => {
  // 권한 변경 (view/edit)
  res.status(501).json({ error: 'Not implemented (S7)' });
});

router.delete('/:id', authMiddleware, (req, res) => {
  // 공유 해제
  res.status(501).json({ error: 'Not implemented (S7)' });
});

module.exports = router;
