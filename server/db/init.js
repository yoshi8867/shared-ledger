require('dotenv').config({ path: require('path').join(__dirname, '../../.env') });
require('dotenv').config();
const { pool } = require('./pool');
const fs = require('fs');
const path = require('path');

async function init() {
  if (!pool) {
    console.error('DATABASE_URL not set');
    process.exit(1);
  }
  const sql = fs.readFileSync(path.join(__dirname, 'schema.sql'), 'utf8');
  await pool.query(sql);
  console.log('✅ Schema applied to Neon DB');
  await pool.end();
}

init().catch(err => {
  console.error('❌ Schema apply failed:', err.message);
  process.exit(1);
});
