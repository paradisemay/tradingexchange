CREATE INDEX IF NOT EXISTS idx_orders_user_created_at
    ON orders(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_status_created_at
    ON orders(status, created_at);

CREATE INDEX IF NOT EXISTS idx_transactions_user_created_at
    ON transactions(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_positions_user
    ON portfolio_positions(user_id);

CREATE INDEX IF NOT EXISTS idx_sessions_user_active
    ON sessions(user_id, expires_at)
    WHERE revoked_at IS NULL;
