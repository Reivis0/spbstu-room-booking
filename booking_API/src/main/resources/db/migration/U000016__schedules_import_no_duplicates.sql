-- U000015__schedules_import_no_duplicates.sql
ALTER TABLE schedules_import DROP CONSTRAINT IF EXISTS uq_schedules_import_room_time;
