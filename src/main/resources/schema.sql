CREATE TABLE IF NOT EXISTS conversation_state (
    conversation_id VARCHAR(128) NOT NULL,
    state_key       VARCHAR(255) NOT NULL,
    state_value     VARCHAR(255) NOT NULL,
    expires_at      TIMESTAMP    NOT NULL,

    PRIMARY KEY (conversation_id, state_key)
);

CREATE INDEX idx_conversation_state_expires ON conversation_state (expires_at);