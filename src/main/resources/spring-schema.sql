CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE vector_store (
    id TEXT PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding VECTOR(1536),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Recommended indexes
CREATE INDEX idx_vector_store_embedding
    ON vector_store
    USING HNSW (embedding)
    WITH (m = 16, ef_construction = 64);

CREATE INDEX idx_vector_store_metadata
    ON vector_store
    USING GIN (metadata);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    payment_id VARCHAR(255) NOT NULL UNIQUE,
    customer_id VARCHAR(255) NOT NULL,
    bill_id VARCHAR(255) NOT NULL,
    merchant_id VARCHAR(255),
    currency VARCHAR(50),
    amount NUMERIC(14, 2) NOT NULL,
    payment_type VARCHAR(30) NOT NULL,
    scheduled_date DATE,
    status VARCHAR(30) NOT NULL,
    idempotency_key VARCHAR(255) UNIQUE,
    gateway_reference VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Indexes
CREATE INDEX idx_payments_idempotency_key
    ON payments(idempotency_key);

CREATE INDEX idx_payments_payment_id
    ON payments(payment_id);

CREATE INDEX idx_payments_status_scheduled_date
    ON payments(status, scheduled_date);

CREATE TABLE bills (
    id BIGSERIAL PRIMARY KEY,
    customer_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    vendor VARCHAR(200),
    category VARCHAR(30),
    period_start DATE,
    period_end DATE,
    late_fee NUMERIC(14, 2),
    payment_id VARCHAR(100),
    document_url VARCHAR(1000),
    extracted_text TEXT,
    source VARCHAR(50),
    auto_pay_enabled BOOLEAN DEFAULT FALSE,
    auto_pay_scheduled_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Indexes
CREATE INDEX idx_bills_customer_id ON bills(customer_id);
CREATE INDEX idx_bills_status ON bills(status);
CREATE INDEX idx_bills_vendor ON bills(vendor);
