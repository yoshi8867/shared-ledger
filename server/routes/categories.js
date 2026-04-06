const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth');
const { updateCategory, deleteCategory } = require('../controllers/categoryController');

// PATCH /api/categories/:id
router.patch('/:id', authMiddleware, updateCategory);

// DELETE /api/categories/:id
router.delete('/:id', authMiddleware, deleteCategory);

module.exports = router;
