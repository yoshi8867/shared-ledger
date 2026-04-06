const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth');
const { delta, push } = require('../controllers/syncController');

// GET /api/sync/delta?since=2026-04-01T00:00:00Z
router.get('/delta', authMiddleware, delta);

// POST /api/sync/push
router.post('/push', authMiddleware, push);

module.exports = router;
