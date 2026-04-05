require('dotenv').config();
const http = require('http');
const app = require('../app');

const PORT = 3001;

function request(method, path, body, headers = {}) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null;
    const options = {
      hostname: 'localhost',
      port: PORT,
      path,
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...headers,
      },
    };
    const req = http.request(options, (res) => {
      let raw = '';
      res.on('data', chunk => raw += chunk);
      res.on('end', () => resolve({ status: res.statusCode, body: JSON.parse(raw) }));
    });
    req.on('error', reject);
    if (data) req.write(data);
    req.end();
  });
}

async function run() {
  const server = http.createServer(app);
  await new Promise(r => server.listen(PORT, r));
  console.log(`Test server on :${PORT}\n`);

  let pass = 0, fail = 0;

  function check(label, actual, expected) {
    if (actual === expected) {
      console.log(`  ✅ ${label}`);
      pass++;
    } else {
      console.log(`  ❌ ${label} — expected ${expected}, got ${actual}`);
      fail++;
    }
  }

  // 1. signup
  console.log('[1] POST /api/auth/signup');
  const r1 = await request('POST', '/api/auth/signup', { email: 'test@example.com', password: 'pass1234', name: '테스터' });
  check('status 201', r1.status, 201);
  check('access_token 존재', !!r1.body.access_token, true);
  check('refresh_token 존재', !!r1.body.refresh_token, true);
  check('user.email', r1.body.user?.email, 'test@example.com');
  const { access_token, refresh_token } = r1.body;

  // 2. signup duplicate
  console.log('\n[2] POST /api/auth/signup (중복)');
  const r2 = await request('POST', '/api/auth/signup', { email: 'test@example.com', password: 'pass1234', name: '테스터' });
  check('status 409', r2.status, 409);

  // 3. signup validation
  console.log('\n[3] POST /api/auth/signup (잘못된 이메일)');
  const r3 = await request('POST', '/api/auth/signup', { email: 'notanemail', password: 'pass1234', name: '테스터2' });
  check('status 400', r3.status, 400);

  // 4. login
  console.log('\n[4] POST /api/auth/login');
  const r4 = await request('POST', '/api/auth/login', { email: 'test@example.com', password: 'pass1234' });
  check('status 200', r4.status, 200);
  check('access_token 존재', !!r4.body.access_token, true);

  // 5. login wrong password
  console.log('\n[5] POST /api/auth/login (틀린 비밀번호)');
  const r5 = await request('POST', '/api/auth/login', { email: 'test@example.com', password: 'wrongpw' });
  check('status 401', r5.status, 401);

  // 6. verify
  console.log('\n[6] GET /api/auth/verify');
  const r6 = await request('GET', '/api/auth/verify', null, { Authorization: `Bearer ${access_token}` });
  check('status 200', r6.status, 200);
  check('user.email', r6.body.user?.email, 'test@example.com');

  // 7. verify invalid token
  console.log('\n[7] GET /api/auth/verify (invalid token)');
  const r7 = await request('GET', '/api/auth/verify', null, { Authorization: 'Bearer invalidtoken' });
  check('status 401', r7.status, 401);

  // 8. refresh
  console.log('\n[8] POST /api/auth/refresh');
  const r8 = await request('POST', '/api/auth/refresh', { refresh_token });
  check('status 200', r8.status, 200);
  check('access_token 존재', !!r8.body.access_token, true);

  console.log(`\n결과: ${pass} passed, ${fail} failed`);
  server.close();
  process.exit(fail > 0 ? 1 : 0);
}

run().catch(err => { console.error(err); process.exit(1); });
