const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth');
const { updateTransaction, deleteTransaction } = require('../controllers/transactionController');

// PATCH /api/transactions/:id
router.patch('/:id', authMiddleware, updateTransaction);

// DELETE /api/transactions/:id
router.delete('/:id', authMiddleware, deleteTransaction);

module.exports = router;
