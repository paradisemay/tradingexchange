CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE IF NOT EXISTS users (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           CITEXT      UNIQUE NOT NULL,
    password_hash   TEXT        NOT NULL,
    full_name       TEXT,
    role            TEXT        NOT NULL DEFAULT 'CLIENT' CHECK (role IN ('CLIENT', 'ADMIN')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS accounts (
    id               UUID           PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id          UUID           NOT NULL REFERENCES users(id),
    currency         CHAR(3)        NOT NULL DEFAULT 'RUB',
    cash_balance     NUMERIC(20, 4) NOT NULL DEFAULT 0 CHECK (cash_balance >= 0),
    reserved_balance NUMERIC(20, 4) NOT NULL DEFAULT 0 CHECK (reserved_balance >= 0),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, currency)
);

CREATE TABLE IF NOT EXISTS instruments (
    ticker     VARCHAR(12)    PRIMARY KEY,
    name       TEXT           NOT NULL,
    currency   CHAR(3)        NOT NULL DEFAULT 'RUB',
    lot_size   INT            NOT NULL DEFAULT 1 CHECK (lot_size > 0),
    is_active  BOOLEAN        NOT NULL DEFAULT TRUE,
    last_price NUMERIC(20, 4)
);

CREATE TABLE IF NOT EXISTS portfolio_positions (
    user_id    UUID           NOT NULL REFERENCES users(id),
    ticker     VARCHAR(12)    NOT NULL REFERENCES instruments(ticker),
    quantity   NUMERIC(20, 8) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    avg_price  NUMERIC(20, 4) NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    PRIMARY KEY(user_id, ticker)
);

CREATE TABLE IF NOT EXISTS orders (
    id             UUID           PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id        UUID           NOT NULL REFERENCES users(id),
    ticker         VARCHAR(12)    NOT NULL REFERENCES instruments(ticker),
    side           TEXT           NOT NULL CHECK (side IN ('BUY', 'SELL')),
    order_type     TEXT           NOT NULL CHECK (order_type IN ('MARKET', 'LIMIT')),
    status         TEXT           NOT NULL CHECK (status IN ('NEW', 'FILLED', 'CANCELLED', 'REJECTED')),
    quantity       NUMERIC(20, 8) NOT NULL CHECK (quantity > 0),
    price          NUMERIC(20, 4),
    executed_price NUMERIC(20, 4),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS transactions (
    id         UUID           PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id    UUID           NOT NULL REFERENCES users(id),
    order_id   UUID           REFERENCES orders(id),
    ticker     VARCHAR(12)    REFERENCES instruments(ticker),
    type       TEXT           NOT NULL CHECK (type IN ('DEPOSIT', 'WITHDRAW', 'BUY', 'SELL', 'FEE')),
    amount     NUMERIC(20, 4) NOT NULL,
    quantity   NUMERIC(20, 8),
    created_at TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sessions (
    id                 UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id            UUID        NOT NULL REFERENCES users(id),
    refresh_token_hash TEXT        NOT NULL,
    expires_at         TIMESTAMPTZ NOT NULL,
    revoked_at         TIMESTAMPTZ
);
