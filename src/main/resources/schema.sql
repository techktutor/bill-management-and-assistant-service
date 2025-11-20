CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE document_embeddings (
    id BIGSERIAL PRIMARY KEY,
    doc_id UUID DEFAULT uuid_generate_v4() NOT NULL,
    source VARCHAR(255),
    chunk_text TEXT,
    embedding VECTOR(768),
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX ON document_embeddings USING HNSW (embedding vector_cosine_ops);

-- PostgreSQL schema for Stripe-like Payment Service

-- --- payments table ---
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    customer_id UUID,
    amount BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_intent_id VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- --- idempotency_key table ---
CREATE TABLE idempotency_key (
    key VARCHAR(255) PRIMARY KEY,
    response_snapshot TEXT,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP
);

-- --- ledger_entries table ---
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    payment_id UUID REFERENCES payments(id),
    account_id UUID NOT NULL,
    amount BIGINT NOT NULL,
    entry_type VARCHAR(20) NOT NULL, -- DEBIT or CREDIT
    balance_snapshot BIGINT,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL
);

-- --- customers table (optional) ---
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    name VARCHAR(255),
    email VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

-- --- card_tokens table (for tokenization) ---
CREATE TABLE card_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    last4 VARCHAR(4) NOT NULL,
    brand VARCHAR(20),
    exp_month INT,
    exp_year INT,
    fingerprint VARCHAR(255),
    encrypted_payload BYTEA, -- optional
    created_at TIMESTAMP NOT NULL
);

-- --- webhook_subscriptions table ---
CREATE TABLE webhook_subscriptions (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- --- webhook_events table ---
CREATE TABLE webhook_events (
    id UUID PRIMARY KEY,
    webhook_subscription_id UUID REFERENCES webhook_subscriptions(id),
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, SENT, FAILED
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

-- --- outbox_events table (for reliable external calls) ---
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_payments_merchant ON payments(merchant_id);
CREATE UNIQUE INDEX uidx_payments_payment_intent_id ON payments(payment_intent_id);
CREATE INDEX idx_scheduled_payments_date_status ON scheduled_payments(scheduled_date, status);
