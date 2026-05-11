CREATE TABLE IF NOT EXISTS booking_saga (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    state TEXT NOT NULL,
    current_step TEXT NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 5,
    next_retry_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_error TEXT,
    trace_id TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_booking_saga_state
        CHECK (state IN ('PENDING', 'PROCESSING', 'CONFIRMED', 'COMPENSATED')),
    CONSTRAINT chk_booking_saga_step
        CHECK (current_step IN ('LIMITS', 'AVAILABILITY', 'COMPLETED', 'COMPENSATED'))
);

CREATE INDEX IF NOT EXISTS idx_booking_saga_due
    ON booking_saga (next_retry_at, created_at)
    WHERE state IN ('PENDING', 'PROCESSING');

ALTER TABLE outbox_events
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS publish_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_error TEXT,
    ADD COLUMN IF NOT EXISTS trace_id TEXT;

CREATE INDEX IF NOT EXISTS idx_outbox_unpublished_attempts
    ON outbox_events (created_at, publish_attempts)
    WHERE published = false;

DROP TRIGGER IF EXISTS trg_booking_saga_updated_at ON booking_saga;
CREATE TRIGGER trg_booking_saga_updated_at
    BEFORE UPDATE ON booking_saga
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
