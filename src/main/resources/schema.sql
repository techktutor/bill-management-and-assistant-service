CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;

CREATE TABLE IF NOT EXISTS vector_store (
  id UUID PRIMARY KEY,
  content TEXT NOT NULL,
  metadata JSONB,
  embedding VECTOR(768)
);

-- HNSW index for similarity search
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
  ON vector_store
  USING HNSW (embedding vector_cosine_ops)
  WITH (m = 16, ef_construction = 64);

-- Metadata filtering
CREATE INDEX IF NOT EXISTS idx_vector_store_metadata
  ON vector_store
  USING GIN (metadata);

CREATE TABLE IF NOT EXISTS bills (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,

    consumer_id VARCHAR(255),
    consumer_name VARCHAR(255),

    category VARCHAR(32) NOT NULL,
    provider_name VARCHAR(255),
    service_number VARCHAR(255),

    billing_start_date DATE,
    billing_end_date DATE,

    amount_due NUMERIC(12,2) NOT NULL,
    currency CHAR(3) NOT NULL,

    due_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,

    payment_id UUID,   -- logical reference ONLY (no FK)

    ingested_at TIMESTAMP WITH TIME ZONE,

    confidence_score INTEGER,
    confidence_decision VARCHAR(32),

    chunk_count INTEGER DEFAULT 0,

    metadata JSONB,

    version BIGINT NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bill_user ON bills (user_id);
CREATE INDEX IF NOT EXISTS idx_bill_consumer ON bills (consumer_id);
CREATE INDEX IF NOT EXISTS idx_bill_consumer_name ON bills (consumer_name);
CREATE INDEX IF NOT EXISTS idx_bill_category ON bills (category);
CREATE INDEX IF NOT EXISTS idx_bill_due_date ON bills (due_date);
CREATE INDEX IF NOT EXISTS idx_bill_status ON bills (status);
CREATE INDEX IF NOT EXISTS idx_bill_payment ON bills (payment_id);
CREATE INDEX IF NOT EXISTS idx_bill_provider_name ON bills (provider_name);

CREATE TABLE IF NOT EXISTS payments (
    payment_id UUID PRIMARY KEY,

    user_id UUID NOT NULL,
    bill_id UUID NOT NULL,  -- logical reference ONLY (no FK)

    currency CHAR(3) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,

    payment_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,

    scheduled_date DATE,

    idempotency_key VARCHAR(64) NOT NULL UNIQUE,

    executed_by VARCHAR(20),

    approved_at TIMESTAMP WITH TIME ZONE,
    execute_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,

    payment_reference VARCHAR(255) UNIQUE,
    gateway_reference_id VARCHAR(255),

    gateway_payload JSONB,
    failure_reason VARCHAR(255),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_payments_payment_id
    ON payments (payment_id);

CREATE INDEX IF NOT EXISTS idx_payments_user_id
    ON payments (user_id);

CREATE INDEX IF NOT EXISTS idx_payments_bill_id
    ON payments (bill_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_payments_idempotency_key
    ON payments (idempotency_key);

CREATE INDEX IF NOT EXISTS idx_payments_status
    ON payments (status);

CREATE INDEX IF NOT EXISTS idx_payments_scheduled_date
    ON payments (scheduled_date);

CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY,

    -- Identity
    external_customer_id VARCHAR(64) UNIQUE,     -- ID from upstream system / CRM
    user_id UUID,                                -- link to auth/user service (optional)

    -- Personal / Business Info
    customer_type VARCHAR(20) NOT NULL,           -- INDIVIDUAL | BUSINESS
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    full_name VARCHAR(200),
    company_name VARCHAR(255),

    -- Contact Info
    email VARCHAR(255),
    phone_country_code VARCHAR(5),
    phone_number VARCHAR(20),

    -- Address (denormalized for performance)
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(2),                           -- ISO-3166 alpha-2

    -- Status & Lifecycle
    status VARCHAR(32) NOT NULL,                  -- ACTIVE | INACTIVE | SUSPENDED | CLOSED
    verified BOOLEAN NOT NULL DEFAULT FALSE,

    -- Compliance / Risk
    kyc_status VARCHAR(32),                       -- PENDING | VERIFIED | FAILED
    risk_score INTEGER,                           -- optional risk scoring

    -- Flexible attributes
    metadata JSONB,

    -- Optimistic locking
    version BIGINT NOT NULL,

    -- Auditing
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE            -- soft delete
);

-- Lookup
CREATE INDEX IF NOT EXISTS idx_customers_user_id
    ON customers (user_id);

CREATE INDEX IF NOT EXISTS idx_customers_external_id
    ON customers (external_customer_id);

-- Search & filtering
CREATE INDEX IF NOT EXISTS idx_customers_email
    ON customers (email);

CREATE INDEX IF NOT EXISTS idx_customers_phone
    ON customers (phone_country_code, phone_number);

CREATE INDEX IF NOT EXISTS idx_customers_status
    ON customers (status);

-- Soft delete filtering
CREATE INDEX IF NOT EXISTS idx_customers_active
    ON customers (deleted_at)
    WHERE deleted_at IS NULL;

-- Metadata search
CREATE INDEX IF NOT EXISTS idx_customers_metadata
    ON customers
    USING GIN (metadata);

CREATE TABLE IF NOT EXISTS user_context (
    context_id        UUID        PRIMARY KEY,
    user_id           UUID        NOT NULL,
    conversation_id   UUID        NOT NULL,
    last_access_time  BIGINT      NOT NULL,
    version           BIGINT      NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_context_user
    ON user_context (user_id);

CREATE INDEX IF NOT EXISTS idx_user_context_last_access
    ON user_context (last_access_time);


