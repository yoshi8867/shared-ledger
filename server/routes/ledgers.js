const express = require('express');
const router = express.Router();
const { pool } = require('../db/pool');
const authMiddleware = require('../middleware/auth');
const { checkRead, checkEdit } = require('../middleware/ledgerAccess');
const { getTransactions, createTransaction } = require('../controllers/transactionController');
const { getCategories, createCategory } = require('../controllers/categoryController');

// GET /api/ledgers — 내 장부 목록
router.get('/', authMiddleware, async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT * FROM ledgers WHERE owner_id = $1 ORDER BY created_at ASC`,
      [req.user.user_id]
    );
    res.json({ ledgers: result.rows });
  } catch (err) {
    console.error('[getLedgers]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /api/ledgers — 장부 생성
router.post('/', authMiddleware, async (req, res) => {
  const { ledger_name } = req.body;
  try {
    const result = await pool.query(
      `INSERT INTO ledgers (owner_id, ledger_name) VALUES ($1, $2) RETURNING *`,
      [req.user.user_id, ledger_name || '내 장부']
    );
    res.status(201).json({ ledger: result.rows[0] });
  } catch (err) {
    console.error('[createLedger]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /api/ledgers/shared — 나와 공유된 장부 목록 (S7)
router.get('/shared', authMiddleware, (req, res) => {
  res.status(501).json({ error: 'Not implemented (S7)' });
});

// GET /api/ledgers/:id — 장부 단건 조회
router.get('/:id', authMiddleware, checkRead, async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT * FROM ledgers WHERE ledger_id = $1`,
      [req.params.id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: '장부를 찾을 수 없습니다' });
    }
    res.json({ ledger: result.rows[0] });
  } catch (err) {
    console.error('[getLedger]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// PUT /api/ledgers/:id — 장부 수정
router.put('/:id', authMiddleware, checkEdit, async (req, res) => {
  const { ledger_name } = req.body;
  try {
    const result = await pool.query(
      `UPDATE ledgers SET ledger_name = COALESCE($1, ledger_name), updated_at = NOW()
       WHERE ledger_id = $2 RETURNING *`,
      [ledger_name || null, req.params.id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: '장부를 찾을 수 없습니다' });
    }
    res.json({ ledger: result.rows[0] });
  } catch (err) {
    console.error('[updateLedger]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// 거래 중첩 라우트
router.get('/:ledgerId/transactions', authMiddleware, checkRead, getTransactions);
router.post('/:ledgerId/transactions', authMiddleware, checkEdit, createTransaction);

// 카테고리 중첩 라우트
router.get('/:ledgerId/categories', authMiddleware, checkRead, getCategories);
router.post('/:ledgerId/categories', authMiddleware, checkEdit, createCategory);

module.exports = router;
