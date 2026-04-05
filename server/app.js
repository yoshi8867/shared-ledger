const express = require('express');
const cors = require('cors');
const morgan = require('morgan');

const healthRouter = require('./routes/health');
const authRouter = require('./routes/auth');
const transactionsRouter = require('./routes/transactions');
const categoriesRouter = require('./routes/categories');
const ledgersRouter = require('./routes/ledgers');
const syncRouter = require('./routes/sync');
const sharedRouter = require('./routes/shared');
const settingsRouter = require('./routes/settings');

const app = express();

app.use(cors());
app.use(express.json());
app.use(morgan('dev'));

app.use('/api', healthRouter);
app.use('/api/auth', authRouter);
app.use('/api/transactions', transactionsRouter);
app.use('/api/categories', categoriesRouter);
app.use('/api/ledgers', ledgersRouter);
app.use('/api/sync', syncRouter);
app.use('/api/shared-ledgers', sharedRouter);
app.use('/api/settings', settingsRouter);

// 404
app.use((req, res) => {
  res.status(404).json({ error: `Cannot ${req.method} ${req.path}` });
});

// 500
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ error: 'Internal server error' });
});

module.exports = app;
