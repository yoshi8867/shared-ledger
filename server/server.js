require('dotenv').config({ path: require('path').join(__dirname, '.env') });
const app = require('./app');
const { pool } = require('./db/pool');

const PORT = process.env.PORT || 3000;

async function migrate() {
  await pool.query(`ALTER TABLE ledgers ADD COLUMN IF NOT EXISTS invite_code VARCHAR(8) UNIQUE`);
  await pool.query(`ALTER TABLE ledgers ADD COLUMN IF NOT EXISTS invite_expires_at TIMESTAMPTZ`);
  await pool.query(`
    CREATE INDEX IF NOT EXISTS idx_ledgers_invite_code
      ON ledgers(invite_code)
      WHERE invite_code IS NOT NULL
  `);
  console.log('✅ Migration applied');
}

async function start() {
  if (process.env.DATABASE_URL) {
    try {
      await pool.query('SELECT 1');
      console.log('✅ DB connected');
      await migrate();
    } catch (err) {
      console.warn('⚠️  DB connection failed:', err.message);
    }
  } else {
    console.warn('⚠️  DATABASE_URL not set — DB features unavailable (health endpoint still works)');
  }

  app.listen(PORT, () => {
    console.log(`🚀 Server running on http://localhost:${PORT}`);
    console.log(`   Health: http://localhost:${PORT}/api/health`);
  });
}

start();
