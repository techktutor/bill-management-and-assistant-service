CREATE TABLE IF NOT EXISTS conversation_state (
    conversation_id VARCHAR(128) PRIMARY KEY,
    user_id         VARCHAR(64) NOT NULL,
    state           VARCHAR(255) NOT NULL,
    bill_id         VARCHAR(64),
    amount          DECIMAL(12,2),
    expires_at      TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_conversation_state ON conversation_state (expires_at);
