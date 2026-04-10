const { pool } = require('../db/pool');

/** 사용자의 주 장부 ID 조회 (없으면 null) */
async function getPrimaryLedgerId(userId) {
  const result = await pool.query(
    'SELECT ledger_id FROM ledgers WHERE owner_id = $1 ORDER BY created_at ASC LIMIT 1',
    [userId]
  );
  return result.rows[0]?.ledger_id ?? null;
}

// GET /api/sync/delta?since=2026-04-01T00:00:00Z
async function delta(req, res) {
  const { since } = req.query;
  const userId = req.user.user_id;

  if (!since) {
    return res.status(400).json({ error: 'since 파라미터가 필요합니다 (ISO 8601)' });
  }

  try {
    const ledgerId = await getPrimaryLedgerId(userId);
    if (!ledgerId) return res.status(404).json({ error: '장부를 찾을 수 없습니다' });

    const serverTime = new Date().toISOString();

    const [txResult, catResult] = await Promise.all([
      pool.query(
        `SELECT transaction_id, ledger_id, created_by, amount, type, category_id,
                description, transaction_date, transaction_time,
                is_deleted, deleted_at, created_at, updated_at
         FROM transactions
         WHERE ledger_id = $1 AND updated_at > $2
         ORDER BY updated_at ASC`,
        [ledgerId, since]
      ),
      pool.query(
        `SELECT category_id, ledger_id, category_name, color, type,
                is_deleted, deleted_at, created_at, updated_at
         FROM categories
         WHERE ledger_id = $1 AND updated_at > $2
         ORDER BY updated_at ASC`,
        [ledgerId, since]
      ),
    ]);

    res.json({
      server_time: serverTime,
      ledger_id: ledgerId,
      transactions: txResult.rows,
      categories: catResult.rows,
    });
  } catch (err) {
    console.error('[sync/delta]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// POST /api/sync/push
async function push(req, res) {
  const { transactions = [], categories = [] } = req.body;
  const userId = req.user.user_id;

  try {
    const ledgerId = await getPrimaryLedgerId(userId);
    if (!ledgerId) return res.status(404).json({ error: '장부를 찾을 수 없습니다' });

    const catResults = [];
    const txResults = [];

    // ── 카테고리 먼저 처리 (거래가 category_id를 참조하므로) ──────────────
    for (const cat of categories) {
      const { client_id, server_id, category_name, color, type, is_deleted, updated_at } = cat;
      const categoryType = (type === 'income' || type === 'expense') ? type : 'expense';

      if (is_deleted && server_id) {
        await pool.query(
          `UPDATE categories
           SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW()
           WHERE category_id = $1 AND ledger_id = $2`,
          [server_id, ledgerId]
        );
        catResults.push({ client_id, server_id, server_updated_at: new Date().toISOString() });

      } else if (!server_id) {
        // 신규
        const r = await pool.query(
          `INSERT INTO categories (ledger_id, category_name, color, type)
           VALUES ($1, $2, $3, $4)
           RETURNING category_id, updated_at`,
          [ledgerId, category_name, color ?? null, categoryType]
        );
        catResults.push({
          client_id,
          server_id: r.rows[0].category_id,
          server_updated_at: r.rows[0].updated_at,
        });

      } else {
        // 수정 — 최신 updated_at 우선
        const existing = await pool.query(
          'SELECT updated_at FROM categories WHERE category_id = $1 AND ledger_id = $2',
          [server_id, ledgerId]
        );
        if (existing.rows.length > 0) {
          if (new Date(updated_at) > new Date(existing.rows[0].updated_at)) {
            await pool.query(
              `UPDATE categories
               SET category_name = $1, color = $2, type = $3, updated_at = NOW()
               WHERE category_id = $4`,
              [category_name, color ?? null, categoryType, server_id]
            );
          }
        }
        catResults.push({ client_id, server_id, server_updated_at: new Date().toISOString() });
      }
    }

    // ── 거래 처리 ─────────────────────────────────────────────────────────
    for (const tx of transactions) {
      const {
        client_id, server_id, amount, type, category_server_id,
        description, transaction_date, transaction_time,
        is_deleted, updated_at,
      } = tx;

      if (is_deleted && server_id) {
        await pool.query(
          `UPDATE transactions
           SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW()
           WHERE transaction_id = $1 AND ledger_id = $2`,
          [server_id, ledgerId]
        );
        txResults.push({ client_id, server_id, server_updated_at: new Date().toISOString() });

      } else if (!server_id) {
        // 신규
        const r = await pool.query(
          `INSERT INTO transactions
             (ledger_id, created_by, amount, type, category_id,
              description, transaction_date, transaction_time)
           VALUES ($1,$2,$3,$4,$5,$6,$7,$8)
           RETURNING transaction_id, updated_at`,
          [ledgerId, userId, amount, type, category_server_id ?? null,
           description ?? null, transaction_date, transaction_time ?? null]
        );
        txResults.push({
          client_id,
          server_id: r.rows[0].transaction_id,
          server_updated_at: r.rows[0].updated_at,
        });

      } else {
        // 수정 — 최신 updated_at 우선
        const existing = await pool.query(
          'SELECT updated_at FROM transactions WHERE transaction_id = $1 AND ledger_id = $2',
          [server_id, ledgerId]
        );
        if (existing.rows.length > 0) {
          if (new Date(updated_at) > new Date(existing.rows[0].updated_at)) {
            await pool.query(
              `UPDATE transactions
               SET amount=$1, type=$2, category_id=$3, description=$4,
                   transaction_date=$5, transaction_time=$6, updated_at=NOW()
               WHERE transaction_id=$7`,
              [amount, type, category_server_id ?? null, description ?? null,
               transaction_date, transaction_time ?? null, server_id]
            );
          }
        }
        txResults.push({ client_id, server_id, server_updated_at: new Date().toISOString() });
      }
    }

    res.json({ transactions: txResults, categories: catResults });
  } catch (err) {
    console.error('[sync/push]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { delta, push };
