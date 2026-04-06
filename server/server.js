require('dotenv').config({ path: require('path').join(__dirname, '.env') });
const app = require('./app');
const { pool } = require('./db/pool');

const PORT = process.env.PORT || 3000;

async function start() {
  if (process.env.DATABASE_URL) {
    try {
      await pool.query('SELECT 1');
      console.log('✅ DB connected');
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
