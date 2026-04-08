const express = require('express');
const router = express.Router();
const { signup, login, refresh, verify, loginWithGoogle } = require('../controllers/authController');
const authMiddleware = require('../middleware/auth');

router.post('/signup', signup);
router.post('/login', login);
router.post('/refresh', refresh);
router.get('/verify', authMiddleware, verify);

router.post('/oauth/google', loginWithGoogle);
// S4에서 구현 예정: Naver OAuth
router.post('/oauth/naver', (req, res) => {
  res.status(501).json({ error: 'Not implemented (S4)' });
});

module.exports = router;
