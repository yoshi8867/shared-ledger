const express = require('express');
const router = express.Router();
const { signup, login, refresh, verify, loginWithGoogle, loginWithNaver } = require('../controllers/authController');
const authMiddleware = require('../middleware/auth');

router.post('/signup', signup);
router.post('/login', login);
router.post('/refresh', refresh);
router.get('/verify', authMiddleware, verify);

router.post('/oauth/google', loginWithGoogle);
router.post('/oauth/naver', loginWithNaver);

module.exports = router;
