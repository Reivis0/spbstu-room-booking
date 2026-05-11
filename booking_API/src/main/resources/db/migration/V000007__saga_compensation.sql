ALTER TYPE booking_status ADD VALUE IF NOT EXISTS 'compensation_pending';
ALTER TYPE booking_status ADD VALUE IF NOT EXISTS 'compensated';

CREATE TABLE IF NOT EXISTS saga_compensation_outbox (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payload JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    processed_at TIMESTAMP,
    retry_count INT DEFAULT 0
);
