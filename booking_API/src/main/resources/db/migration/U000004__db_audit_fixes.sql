DROP TRIGGER IF EXISTS trg_schedules_import_updated_at ON schedules_import;
DROP TRIGGER IF EXISTS trg_buildings_updated_at ON buildings;
DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
DROP TRIGGER IF EXISTS trg_rooms_updated_at ON rooms;
DROP TRIGGER IF EXISTS trg_bookings_updated_at ON bookings;

DROP FUNCTION IF EXISTS set_updated_at();

DROP INDEX IF EXISTS idx_schedules_brin_updated;
DROP INDEX IF EXISTS idx_users_email;
DROP INDEX IF EXISTS idx_schedules_hash;
DROP INDEX IF EXISTS idx_outbox_unpublished;
DROP INDEX IF EXISTS idx_bookings_confirmed;
DROP INDEX IF EXISTS idx_bookings_room_tsrange;

DROP TABLE IF EXISTS outbox_events;

ALTER TABLE buildings DROP COLUMN IF EXISTS updated_at;
ALTER TABLE rooms DROP COLUMN IF EXISTS updated_at;
ALTER TABLE bookings DROP COLUMN IF EXISTS version;

ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_user_id_fkey;
