require('dotenv').config();
const http = require('http');
const app = require('../app');

const PORT = 3002;

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
      res.on('data', (chunk) => (raw += chunk));
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, body: raw ? JSON.parse(raw) : null });
        } catch {
          resolve({ status: res.statusCode, body: raw });
        }
      });
    });
    req.on('error', reject);
    if (data) req.write(data);
    req.end();
  });
}

async function run() {
  const server = http.createServer(app);
  await new Promise((r) => server.listen(PORT, r));
  console.log(`Test server on :${PORT}\n`);

  let pass = 0, fail = 0;
  let token = '';
  let ledgerId = null;
  let categoryId = null;
  let transactionId = null;

  function check(label, actual, expected) {
    if (actual === expected) {
      console.log(`  ✅ ${label}`);
      pass++;
    } else {
      console.log(`  ❌ ${label} — expected ${expected}, got ${actual}`);
      fail++;
    }
  }

  // ── 사전 준비: 회원가입 + 토큰 획득 ──────────────────────────────────
  console.log('[준비] 회원가입 → 토큰 획득');
  const email = `crud_test_${Date.now()}@test.com`;
  const signupRes = await request('POST', '/api/auth/signup', {
    email,
    password: 'pass1234',
    name: 'CRUD Tester',
  });
  token = signupRes.body?.access_token;
  check('signup 201', signupRes.status, 201);
  check('access_token 존재', !!token, true);
  console.log('');

  const auth = { Authorization: `Bearer ${token}` };

  // ── 1. 내 장부 목록 조회 ──────────────────────────────────────────────
  console.log('[1] GET /api/ledgers');
  const ledgersRes = await request('GET', '/api/ledgers', null, auth);
  check('200 OK', ledgersRes.status, 200);
  check('기본 장부 1개 생성됨', Array.isArray(ledgersRes.body?.ledgers) && ledgersRes.body.ledgers.length >= 1, true);
  ledgerId = ledgersRes.body?.ledgers[0]?.ledger_id;
  check('ledger_id 존재', !!ledgerId, true);
  console.log(`  → ledger_id: ${ledgerId}`);
  console.log('');

  // ── 2. 카테고리 생성 ──────────────────────────────────────────────────
  console.log('[2] POST /api/ledgers/:ledgerId/categories');
  const catRes = await request('POST', `/api/ledgers/${ledgerId}/categories`, {
    category_name: '식비',
    color: '#FF5722',
  }, auth);
  check('201 Created', catRes.status, 201);
  check('category_name 반환', catRes.body?.category?.category_name, '식비');
  check('color 반환', catRes.body?.category?.color, '#FF5722');
  categoryId = catRes.body?.category?.category_id;
  check('category_id 존재', !!categoryId, true);
  console.log(`  → category_id: ${categoryId}`);
  console.log('');

  // ── 3. 카테고리 생성 — 유효성 검사 ──────────────────────────────────
  console.log('[3] POST /api/ledgers/:ledgerId/categories — 유효성 오류');
  const catBadColorRes = await request('POST', `/api/ledgers/${ledgerId}/categories`, {
    category_name: '교통',
    color: 'red',
  }, auth);
  check('잘못된 color → 400', catBadColorRes.status, 400);

  const catNoNameRes = await request('POST', `/api/ledgers/${ledgerId}/categories`, {
    color: '#FFFFFF',
  }, auth);
  check('이름 누락 → 400', catNoNameRes.status, 400);
  console.log('');

  // ── 4. 카테고리 목록 조회 ────────────────────────────────────────────
  console.log('[4] GET /api/ledgers/:ledgerId/categories');
  const catsRes = await request('GET', `/api/ledgers/${ledgerId}/categories`, null, auth);
  check('200 OK', catsRes.status, 200);
  check('카테고리 1개 이상', Array.isArray(catsRes.body?.categories) && catsRes.body.categories.length >= 1, true);
  console.log('');

  // ── 5. 거래 생성 ──────────────────────────────────────────────────────
  console.log('[5] POST /api/ledgers/:ledgerId/transactions');
  const txRes = await request('POST', `/api/ledgers/${ledgerId}/transactions`, {
    amount: 12000,
    type: 'expense',
    category_id: categoryId,
    description: '점심 식사',
    transaction_date: '2026-04-06',
    transaction_time: '12:30:00',
  }, auth);
  check('201 Created', txRes.status, 201);
  check('amount 반환', Number(txRes.body?.transaction?.amount), 12000);
  check('type 반환', txRes.body?.transaction?.type, 'expense');
  transactionId = txRes.body?.transaction?.transaction_id;
  check('transaction_id 존재', !!transactionId, true);
  console.log(`  → transaction_id: ${transactionId}`);
  console.log('');

  // ── 6. 거래 생성 — 유효성 검사 ───────────────────────────────────────
  console.log('[6] POST /api/ledgers/:ledgerId/transactions — 유효성 오류');
  const txBadTypeRes = await request('POST', `/api/ledgers/${ledgerId}/transactions`, {
    amount: 5000,
    type: 'invalid',
    transaction_date: '2026-04-06',
  }, auth);
  check('잘못된 type → 400', txBadTypeRes.status, 400);

  const txNegAmtRes = await request('POST', `/api/ledgers/${ledgerId}/transactions`, {
    amount: -100,
    type: 'income',
    transaction_date: '2026-04-06',
  }, auth);
  check('음수 amount → 400', txNegAmtRes.status, 400);

  const txMissingRes = await request('POST', `/api/ledgers/${ledgerId}/transactions`, {
    type: 'expense',
  }, auth);
  check('필수 필드 누락 → 400', txMissingRes.status, 400);
  console.log('');

  // ── 7. 거래 목록 조회 (월별) ─────────────────────────────────────────
  console.log('[7] GET /api/ledgers/:ledgerId/transactions?month=2026-04');
  const txListRes = await request('GET', `/api/ledgers/${ledgerId}/transactions?month=2026-04`, null, auth);
  check('200 OK', txListRes.status, 200);
  check('거래 1개 이상', Array.isArray(txListRes.body?.transactions) && txListRes.body.transactions.length >= 1, true);

  const txBadMonthRes = await request('GET', `/api/ledgers/${ledgerId}/transactions?month=2026-4`, null, auth);
  check('잘못된 month 형식 → 400', txBadMonthRes.status, 400);
  console.log('');

  // ── 8. 거래 수정 ─────────────────────────────────────────────────────
  console.log('[8] PATCH /api/transactions/:id');
  const txUpdateRes = await request('PATCH', `/api/transactions/${transactionId}`, {
    description: '점심 식사 (수정됨)',
    amount: 15000,
  }, auth);
  check('200 OK', txUpdateRes.status, 200);
  check('description 수정됨', txUpdateRes.body?.transaction?.description, '점심 식사 (수정됨)');
  check('amount 수정됨', Number(txUpdateRes.body?.transaction?.amount), 15000);
  console.log('');

  // ── 9. 카테고리 수정 ─────────────────────────────────────────────────
  console.log('[9] PATCH /api/categories/:id');
  const catUpdateRes = await request('PATCH', `/api/categories/${categoryId}`, {
    category_name: '외식',
    color: '#E91E63',
  }, auth);
  check('200 OK', catUpdateRes.status, 200);
  check('category_name 수정됨', catUpdateRes.body?.category?.category_name, '외식');
  check('color 수정됨', catUpdateRes.body?.category?.color, '#E91E63');
  console.log('');

  // ── 10. 권한 없는 장부 접근 ──────────────────────────────────────────
  console.log('[10] 권한 거부 케이스');
  const otherSignupRes = await request('POST', '/api/auth/signup', {
    email: `other_${Date.now()}@test.com`,
    password: 'pass1234',
    name: '다른 유저',
  });
  const otherToken = otherSignupRes.body?.access_token;
  const otherAuth = { Authorization: `Bearer ${otherToken}` };

  const unauthorizedListRes = await request('GET', `/api/ledgers/${ledgerId}/transactions`, null, otherAuth);
  check('타인 장부 조회 → 403', unauthorizedListRes.status, 403);

  const unauthorizedCreateRes = await request('POST', `/api/ledgers/${ledgerId}/transactions`, {
    amount: 1000, type: 'income', transaction_date: '2026-04-06',
  }, otherAuth);
  check('타인 장부 거래 추가 → 403', unauthorizedCreateRes.status, 403);

  const unauthorizedUpdateRes = await request('PATCH', `/api/transactions/${transactionId}`, {
    amount: 999,
  }, otherAuth);
  check('타인 거래 수정 → 403', unauthorizedUpdateRes.status, 403);
  console.log('');

  // ── 11. 거래 소프트 딜리트 ───────────────────────────────────────────
  console.log('[11] DELETE /api/transactions/:id (소프트 딜리트)');
  const txDeleteRes = await request('DELETE', `/api/transactions/${transactionId}`, null, auth);
  check('204 No Content', txDeleteRes.status, 204);

  // 삭제 후 목록에서 제외 확인
  const txAfterDeleteRes = await request('GET', `/api/ledgers/${ledgerId}/transactions?month=2026-04`, null, auth);
  const found = txAfterDeleteRes.body?.transactions?.some(t => t.transaction_id === transactionId);
  check('삭제 후 목록에서 제외됨', found, false);

  // 없는 거래 삭제
  const txNotFoundRes = await request('DELETE', `/api/transactions/${transactionId}`, null, auth);
  check('이미 삭제된 거래 → 404', txNotFoundRes.status, 404);
  console.log('');

  // ── 12. 카테고리 소프트 딜리트 ──────────────────────────────────────
  console.log('[12] DELETE /api/categories/:id (소프트 딜리트)');
  const catDeleteRes = await request('DELETE', `/api/categories/${categoryId}`, null, auth);
  check('204 No Content', catDeleteRes.status, 204);

  const catsAfterRes = await request('GET', `/api/ledgers/${ledgerId}/categories`, null, auth);
  const catFound = catsAfterRes.body?.categories?.some(c => c.category_id === categoryId);
  check('삭제 후 카테고리 목록에서 제외됨', catFound, false);
  console.log('');

  // ── 결과 ─────────────────────────────────────────────────────────────
  console.log(`\n${'='.repeat(40)}`);
  console.log(`결과: ${pass + fail}개 중 ${pass}개 통과, ${fail}개 실패`);
  console.log('='.repeat(40));

  server.close();
  process.exit(fail > 0 ? 1 : 0);
}

run().catch((err) => {
  console.error('테스트 오류:', err);
  process.exit(1);
});
