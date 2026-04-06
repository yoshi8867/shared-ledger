const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth');
const {
  getSharedUsers, invite, updatePermission, revokeAccess
} = require('../controllers/sharedController');

router.get('/',     authMiddleware, getSharedUsers);
router.post('/',    authMiddleware, invite);
router.patch('/:id', authMiddleware, updatePermission);
router.delete('/:id', authMiddleware, revokeAccess);

module.exports = router;
