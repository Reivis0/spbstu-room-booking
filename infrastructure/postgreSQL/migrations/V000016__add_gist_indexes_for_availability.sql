CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE INDEX IF NOT EXISTS idx_schedules_import_gist ON schedules_import USING gist (room_id, tstzrange(starts_at, ends_at));
CREATE INDEX IF NOT EXISTS idx_bookings_gist ON bookings USING gist (room_id, tstzrange(starts_at, ends_at));