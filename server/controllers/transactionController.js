const { pool } = require('../db/pool');

// GET /api/ledgers/:ledgerId/transactions?month=YYYY-MM
async function getTransactions(req, res) {
  const { ledgerId } = req.params;
  const { month } = req.query;

  try {
    let query, params;

    if (month) {
      if (!/^\d{4}-\d{2}$/.test(month)) {
        return res.status(400).json({ error: 'month 파라미터는 YYYY-MM 형식이어야 합니다' });
      }
      query = `
        SELECT t.*, c.category_name, c.color
        FROM transactions t
        LEFT JOIN categories c ON t.category_id = c.category_id
        WHERE t.ledger_id = $1
          AND t.is_deleted = FALSE
          AND TO_CHAR(t.transaction_date, 'YYYY-MM') = $2
        ORDER BY t.transaction_date DESC,
                 t.transaction_time DESC NULLS LAST,
                 t.created_at DESC
      `;
      params = [ledgerId, month];
    } else {
      query = `
        SELECT t.*, c.category_name, c.color
        FROM transactions t
        LEFT JOIN categories c ON t.category_id = c.category_id
        WHERE t.ledger_id = $1
          AND t.is_deleted = FALSE
        ORDER BY t.transaction_date DESC,
                 t.transaction_time DESC NULLS LAST,
                 t.created_at DESC
      `;
      params = [ledgerId];
    }

    const result = await pool.query(query, params);
    res.json({ transactions: result.rows });
  } catch (err) {
    console.error('[getTransactions]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// POST /api/ledgers/:ledgerId/transactions
async function createTransaction(req, res) {
  const { ledgerId } = req.params;
  const userId = req.user.user_id;
  const { amount, type, category_id, description, transaction_date, transaction_time } = req.body;

  if (!amount || !type || !transaction_date) {
    return res.status(400).json({ error: 'amount, type, transaction_date는 필수입니다' });
  }
  if (type !== 'income' && type !== 'expense') {
    return res.status(400).json({ error: 'type은 income 또는 expense여야 합니다' });
  }
  if (isNaN(Number(amount)) || Number(amount) <= 0) {
    return res.status(400).json({ error: 'amount는 양수여야 합니다' });
  }

  try {
    const result = await pool.query(
      `INSERT INTO transactions
         (ledger_id, created_by, amount, type, category_id, description, transaction_date, transaction_time)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING *`,
      [
        ledgerId,
        userId,
        amount,
        type,
        category_id || null,
        description || null,
        transaction_date,
        transaction_time || null,
      ]
    );
    res.status(201).json({ transaction: result.rows[0] });
  } catch (err) {
    console.error('[createTransaction]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// PATCH /api/transactions/:id
async function updateTransaction(req, res) {
  const { id } = req.params;
  const userId = req.user.user_id;
  const { amount, type, category_id, description, transaction_date, transaction_time } = req.body;

  try {
    // 거래 조회
    const txResult = await pool.query(
      `SELECT t.ledger_id FROM transactions t WHERE t.transaction_id = $1 AND t.is_deleted = FALSE`,
      [id]
    );
    if (txResult.rows.length === 0) {
      return res.status(404).json({ error: '거래를 찾을 수 없습니다' });
    }

    const ledgerId = txResult.rows[0].ledger_id;

    // 편집 권한 체크
    const accessResult = await pool.query(
      `SELECT 1 FROM ledgers WHERE ledger_id = $1 AND owner_id = $2
       UNION
       SELECT 1 FROM shared_ledgers WHERE ledger_id = $1 AND shared_with_user_id = $2 AND permission = 'edit'`,
      [ledgerId, userId]
    );
    if (accessResult.rows.length === 0) {
      return res.status(403).json({ error: '이 거래를 수정할 권한이 없습니다' });
    }

    if (type && type !== 'income' && type !== 'expense') {
      return res.status(400).json({ error: 'type은 income 또는 expense여야 합니다' });
    }
    if (amount !== undefined && (isNaN(Number(amount)) || Number(amount) <= 0)) {
      return res.status(400).json({ error: 'amount는 양수여야 합니다' });
    }

    const result = await pool.query(
      `UPDATE transactions SET
         amount           = COALESCE($1, amount),
         type             = COALESCE($2, type),
         category_id      = CASE WHEN $3::BIGINT IS NOT NULL THEN $3::BIGINT ELSE category_id END,
         description      = COALESCE($4, description),
         transaction_date = COALESCE($5::DATE, transaction_date),
         transaction_time = COALESCE($6::TIME, transaction_time),
         updated_at       = NOW()
       WHERE transaction_id = $7
       RETURNING *`,
      [
        amount !== undefined ? amount : null,
        type || null,
        category_id !== undefined ? category_id : null,
        description !== undefined ? description : null,
        transaction_date || null,
        transaction_time !== undefined ? transaction_time : null,
        id,
      ]
    );
    res.json({ transaction: result.rows[0] });
  } catch (err) {
    console.error('[updateTransaction]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// DELETE /api/transactions/:id (소프트 딜리트)
async function deleteTransaction(req, res) {
  const { id } = req.params;
  const userId = req.user.user_id;

  try {
    const txResult = await pool.query(
      `SELECT t.ledger_id FROM transactions t WHERE t.transaction_id = $1 AND t.is_deleted = FALSE`,
      [id]
    );
    if (txResult.rows.length === 0) {
      return res.status(404).json({ error: '거래를 찾을 수 없습니다' });
    }

    const ledgerId = txResult.rows[0].ledger_id;

    // 편집 권한 체크
    const accessResult = await pool.query(
      `SELECT 1 FROM ledgers WHERE ledger_id = $1 AND owner_id = $2
       UNION
       SELECT 1 FROM shared_ledgers WHERE ledger_id = $1 AND shared_with_user_id = $2 AND permission = 'edit'`,
      [ledgerId, userId]
    );
    if (accessResult.rows.length === 0) {
      return res.status(403).json({ error: '이 거래를 삭제할 권한이 없습니다' });
    }

    await pool.query(
      `UPDATE transactions SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW()
       WHERE transaction_id = $1`,
      [id]
    );
    res.status(204).send();
  } catch (err) {
    console.error('[deleteTransaction]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { getTransactions, createTransaction, updateTransaction, deleteTransaction };
