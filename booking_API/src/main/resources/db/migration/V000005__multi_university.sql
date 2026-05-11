-- Migration: V000005__multi_university
-- Description: Support for multiple universities with VARCHAR IDs
-- Author: team

-- STEP 1: Create universities table
CREATE TABLE IF NOT EXISTS universities (
  id          VARCHAR(50) PRIMARY KEY,
  name        VARCHAR(255) NOT NULL,
  ruz_api_url TEXT,
  data_format VARCHAR(10) DEFAULT 'json',
  is_active   BOOLEAN NOT NULL DEFAULT true,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- STEP 2: Add university_id to all relevant tables
ALTER TABLE buildings
  ADD COLUMN IF NOT EXISTS university_id VARCHAR(50) REFERENCES universities(id)
  ON DELETE RESTRICT;

ALTER TABLE rooms
  ADD COLUMN IF NOT EXISTS university_id VARCHAR(50) REFERENCES universities(id);

ALTER TABLE schedules_import
  ADD COLUMN IF NOT EXISTS university_id VARCHAR(50) REFERENCES universities(id) ON DELETE CASCADE;

-- STEP 3: Initial data for universities (metadata only)
INSERT INTO universities (id, name, ruz_api_url, data_format) VALUES
  ('spbptu', 'Санкт-Петербургский политехнический университет Петра Великого', 'https://ruz.spbstu.ru/api/v1/ruz', 'json'),
  ('spbgu',  'Санкт-Петербургский государственный университет', 'https://timetable.spbu.ru/api/v1', 'json'),
  ('leti',   'Санкт-Петербургский государственный электротехнический университет «ЛЭТИ»', 'https://digital.etu.ru/api/mobile/schedule', 'json')
ON CONFLICT (id) DO NOTHING;

-- STEP 4: Add tracking to schedules_import
ALTER TABLE schedules_import
  ADD COLUMN IF NOT EXISTS imported_at TIMESTAMPTZ DEFAULT NOW(),
  ADD COLUMN IF NOT EXISTS api_version VARCHAR(10) DEFAULT 'v1';

-- STEP 5: Indexes
CREATE INDEX IF NOT EXISTS idx_buildings_university ON buildings (university_id);
CREATE INDEX IF NOT EXISTS idx_rooms_university ON rooms (university_id);
CREATE INDEX IF NOT EXISTS idx_schedules_university ON schedules_import (university_id);
CREATE INDEX IF NOT EXISTS idx_schedules_univ_room_tsrange ON schedules_import USING GIST (room_id, tstzrange(starts_at, ends_at));

-- STEP 6: View
CREATE OR REPLACE VIEW v_room_availability AS
SELECT
  r.id            AS room_id,
  r.capacity,
  r.features,
  b.name          AS building_name,
  b.address,
  u.id            AS university_id,
  u.name          AS university_name,
  COUNT(bk.id)    AS active_bookings_count
FROM rooms r
JOIN buildings b ON r.building_id = b.id
JOIN universities u ON b.university_id = u.id
LEFT JOIN bookings bk
  ON bk.room_id = r.id
  AND bk.status = 'confirmed'
  AND bk.ends_at > NOW()
WHERE u.is_active = true
GROUP BY r.id, r.capacity, r.features, b.name, b.address, u.id, u.name;
