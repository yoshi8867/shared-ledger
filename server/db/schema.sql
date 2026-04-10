-- ============================================================
--  Shared Ledger — Neon PostgreSQL Schema
--  Run this file on the Neon console to initialize the database.
--  ID type: BIGSERIAL (auto-increment integer) — Android Room과 Long 타입 호환
-- ============================================================

-- ────────────────────────────────────────────────────────────
-- 1. users
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    user_id       BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),                          -- OAuth 가입자는 NULL
    name          VARCHAR(100) NOT NULL,
    auth_provider VARCHAR(20)  NOT NULL DEFAULT 'email', -- email | google | naver
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ────────────────────────────────────────────────────────────
-- 2. ledgers
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ledgers (
    ledger_id    BIGSERIAL    PRIMARY KEY,
    owner_id     BIGINT       NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    ledger_name  VARCHAR(100) NOT NULL DEFAULT '내 장부',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ────────────────────────────────────────────────────────────
-- 3. categories
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS categories (
    category_id   BIGSERIAL   PRIMARY KEY,
    ledger_id     BIGINT      NOT NULL REFERENCES ledgers(ledger_id) ON DELETE CASCADE,
    category_name VARCHAR(50) NOT NULL,
    color         VARCHAR(7),                           -- hex (예: #FF5722), nullable
    sync_status   VARCHAR(20) NOT NULL DEFAULT 'synced', -- synced | pending | conflict
    is_deleted    BOOLEAN     NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ────────────────────────────────────────────────────────────
-- 4. transactions
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id   BIGSERIAL    PRIMARY KEY,
    ledger_id        BIGINT       NOT NULL REFERENCES ledgers(ledger_id) ON DELETE CASCADE,
    created_by       BIGINT       NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    amount           DECIMAL(15,2) NOT NULL,
    type             VARCHAR(10)  NOT NULL,             -- income | expense
    category_id      BIGINT       REFERENCES categories(category_id) ON DELETE SET NULL,
    description      TEXT,
    transaction_date DATE         NOT NULL,
    transaction_time TIME,
    sync_status      VARCHAR(20)  NOT NULL DEFAULT 'synced', -- synced | pending | conflict
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ────────────────────────────────────────────────────────────
-- 5. shared_ledgers
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS shared_ledgers (
    shared_ledger_id    BIGSERIAL  PRIMARY KEY,
    ledger_id           BIGINT     NOT NULL REFERENCES ledgers(ledger_id) ON DELETE CASCADE,
    shared_with_user_id BIGINT     NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    permission          VARCHAR(10) NOT NULL DEFAULT 'view', -- view | edit
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (ledger_id, shared_with_user_id)
);

-- ────────────────────────────────────────────────────────────
-- 6. system_settings
--    서버 운영자가 동적으로 제어할 설정값 (예: 로그인 필수 여부)
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key   VARCHAR(100) PRIMARY KEY,
    setting_value TEXT         NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 기본값 삽입 (이미 존재하면 무시)
INSERT INTO system_settings (setting_key, setting_value) VALUES
    ('login_required', 'false')
ON CONFLICT (setting_key) DO NOTHING;

-- ────────────────────────────────────────────────────────────
-- 7. Indexes (성능 최적화)
-- ────────────────────────────────────────────────────────────

-- 월별 거래 조회 (ledger + 날짜 범위 필터)
CREATE INDEX IF NOT EXISTS idx_transactions_ledger_date
    ON transactions(ledger_id, transaction_date)
    WHERE is_deleted = FALSE;

-- 동기화 대상 쿼리 (pending 항목만)
CREATE INDEX IF NOT EXISTS idx_transactions_sync
    ON transactions(ledger_id, updated_at)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_categories_ledger
    ON categories(ledger_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_shared_ledgers_user
    ON shared_ledgers(shared_with_user_id);

-- ────────────────────────────────────────────────────────────
-- 8. Invite codes (장부 초대 코드)
-- ────────────────────────────────────────────────────────────
ALTER TABLE ledgers ADD COLUMN IF NOT EXISTS invite_code VARCHAR(8) UNIQUE;
ALTER TABLE ledgers ADD COLUMN IF NOT EXISTS invite_expires_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_ledgers_invite_code
    ON ledgers(invite_code)
    WHERE invite_code IS NOT NULL;

-- Migration: add type column to categories (income | expense, default 'expense')
ALTER TABLE categories ADD COLUMN IF NOT EXISTS type VARCHAR(10) NOT NULL DEFAULT 'expense';
