const { pool } = require('../db/pool');

const COLOR_REGEX = /^#[0-9A-Fa-f]{6}$/;

// GET /api/ledgers/:ledgerId/categories
async function getCategories(req, res) {
  const { ledgerId } = req.params;

  try {
    const result = await pool.query(
      `SELECT * FROM categories WHERE ledger_id = $1 AND is_deleted = FALSE ORDER BY created_at ASC`,
      [ledgerId]
    );
    res.json({ categories: result.rows });
  } catch (err) {
    console.error('[getCategories]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// POST /api/ledgers/:ledgerId/categories
async function createCategory(req, res) {
  const { ledgerId } = req.params;
  const { category_name, color } = req.body;

  if (!category_name || !category_name.trim()) {
    return res.status(400).json({ error: 'category_name은 필수입니다' });
  }
  if (color && !COLOR_REGEX.test(color)) {
    return res.status(400).json({ error: 'color는 #RRGGBB 형식이어야 합니다' });
  }

  try {
    const result = await pool.query(
      `INSERT INTO categories (ledger_id, category_name, color) VALUES ($1, $2, $3) RETURNING *`,
      [ledgerId, category_name.trim(), color || null]
    );
    res.status(201).json({ category: result.rows[0] });
  } catch (err) {
    console.error('[createCategory]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// PATCH /api/categories/:id
async function updateCategory(req, res) {
  const { id } = req.params;
  const userId = req.user.user_id;
  const { category_name, color } = req.body;

  try {
    const catResult = await pool.query(
      `SELECT ledger_id FROM categories WHERE category_id = $1 AND is_deleted = FALSE`,
      [id]
    );
    if (catResult.rows.length === 0) {
      return res.status(404).json({ error: '카테고리를 찾을 수 없습니다' });
    }

    const ledgerId = catResult.rows[0].ledger_id;

    // 편집 권한 체크
    const accessResult = await pool.query(
      `SELECT 1 FROM ledgers WHERE ledger_id = $1 AND owner_id = $2
       UNION
       SELECT 1 FROM shared_ledgers WHERE ledger_id = $1 AND shared_with_user_id = $2 AND permission = 'edit'`,
      [ledgerId, userId]
    );
    if (accessResult.rows.length === 0) {
      return res.status(403).json({ error: '이 카테고리를 수정할 권한이 없습니다' });
    }

    if (color && !COLOR_REGEX.test(color)) {
      return res.status(400).json({ error: 'color는 #RRGGBB 형식이어야 합니다' });
    }

    const result = await pool.query(
      `UPDATE categories SET
         category_name = COALESCE($1, category_name),
         color         = COALESCE($2, color),
         updated_at    = NOW()
       WHERE category_id = $3
       RETURNING *`,
      [category_name ? category_name.trim() : null, color || null, id]
    );
    res.json({ category: result.rows[0] });
  } catch (err) {
    console.error('[updateCategory]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

// DELETE /api/categories/:id (소프트 딜리트)
async function deleteCategory(req, res) {
  const { id } = req.params;
  const userId = req.user.user_id;

  try {
    const catResult = await pool.query(
      `SELECT ledger_id FROM categories WHERE category_id = $1 AND is_deleted = FALSE`,
      [id]
    );
    if (catResult.rows.length === 0) {
      return res.status(404).json({ error: '카테고리를 찾을 수 없습니다' });
    }

    const ledgerId = catResult.rows[0].ledger_id;

    // 편집 권한 체크
    const accessResult = await pool.query(
      `SELECT 1 FROM ledgers WHERE ledger_id = $1 AND owner_id = $2
       UNION
       SELECT 1 FROM shared_ledgers WHERE ledger_id = $1 AND shared_with_user_id = $2 AND permission = 'edit'`,
      [ledgerId, userId]
    );
    if (accessResult.rows.length === 0) {
      return res.status(403).json({ error: '이 카테고리를 삭제할 권한이 없습니다' });
    }

    await pool.query(
      `UPDATE categories SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW()
       WHERE category_id = $1`,
      [id]
    );
    res.status(204).send();
  } catch (err) {
    console.error('[deleteCategory]', err);
    res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { getCategories, createCategory, updateCategory, deleteCategory };
