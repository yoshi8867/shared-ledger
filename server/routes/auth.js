const express = require('express');
const router = express.Router();

// S3에서 구현 예정: 이메일 인증
// S4에서 구현 예정: Google / Naver OAuth

router.post('/signup', (req, res) => {
  res.status(501).json({ error: 'Not implemented (S3)' });
});

router.post('/login', (req, res) => {
  res.status(501).json({ error: 'Not implemented (S3)' });
});

router.post('/refresh', (req, res) => {
  res.status(501).json({ error: 'Not implemented (S3)' });
});

router.get('/verify', (req, res) => {
  res.status(501).json({ error: 'Not implemented (S3)' });
});

router.post('/oauth/google', (req, res) => {
  res.status(501).json({ error: 'Not implemented (S4)' });
});

router.post('/oauth/naver', (req, res) => {
  res.status(501).json({ error: 'Not implemented (S4)' });
});

module.exports = router;
