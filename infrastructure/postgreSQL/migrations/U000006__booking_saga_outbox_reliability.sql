DROP TRIGGER IF EXISTS trg_booking_saga_updated_at ON booking_saga;

DROP INDEX IF EXISTS idx_outbox_unpublished_attempts;
DROP INDEX IF EXISTS idx_booking_saga_due;

ALTER TABLE outbox_events
    DROP COLUMN IF EXISTS trace_id,
    DROP COLUMN IF EXISTS last_error,
    DROP COLUMN IF EXISTS publish_attempts,
    DROP COLUMN IF EXISTS published_at;

DROP TABLE IF EXISTS booking_saga;
