-- U000005__multi_university.sql

-- Откат представления (view)
DROP VIEW IF EXISTS v_room_availability;

-- Откат колонок из schedules_import
ALTER TABLE schedules_import
  DROP COLUMN IF EXISTS university_id,
  DROP COLUMN IF EXISTS imported_at,
  DROP COLUMN IF EXISTS api_version;

-- Откат индексов
DROP INDEX IF EXISTS idx_buildings_university;
DROP INDEX IF EXISTS idx_bookings_confirmed_room_time;
DROP INDEX IF EXISTS idx_schedules_brin_starts;
DROP INDEX IF EXISTS idx_schedules_univ_room_tsrange;

-- Откат внешнего ключа university_id из buildings
ALTER TABLE buildings
  DROP COLUMN IF EXISTS university_id;

-- Откат таблицы universities
DROP TABLE IF EXISTS universities CASCADE;
