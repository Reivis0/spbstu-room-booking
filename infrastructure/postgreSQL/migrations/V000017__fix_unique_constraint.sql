-- V000017__fix_unique_constraint.sql
ALTER TABLE schedules_import DROP CONSTRAINT IF EXISTS uq_schedules_import_room_time;
ALTER TABLE schedules_import ADD CONSTRAINT uq_schedules_import_room_time UNIQUE (room_id, starts_at, ends_at);
