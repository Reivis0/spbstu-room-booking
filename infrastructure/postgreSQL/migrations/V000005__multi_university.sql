-- V000005__multi_university.sql

-- STEP 1: Усовершенствование схемы для поддержки нескольких университетов

-- Создаем таблицу universities
CREATE TABLE IF NOT EXISTS universities (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  code        VARCHAR(20) NOT NULL UNIQUE,  -- 'SPBPU' | 'SPBGU' | 'LETI'
  name        VARCHAR(255) NOT NULL,
  short_name  VARCHAR(50) NOT NULL,
  ruz_api_url TEXT,                         -- базовый URL для API
  is_active   BOOLEAN NOT NULL DEFAULT true,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Добавляем внешний ключ university_id в таблицу buildings
ALTER TABLE buildings
  ADD COLUMN IF NOT EXISTS university_id UUID REFERENCES universities(id)
  ON DELETE RESTRICT;

-- Заполняем начальные данные по университетам
INSERT INTO universities (code, name, short_name, ruz_api_url) VALUES
  ('SPBPU', 'Санкт-Петербургский политехнический университет Петра Великого',
   'СПбПУ', 'https://ruz.spbstu.ru/api/v1/ruz'),
  ('SPBGU', 'Санкт-Петербургский государственный университет',
   'СПбГУ', 'https://timetable.spbu.ru/api/v1'),
  ('LETI',  'Санкт-Петербургский государственный электротехнический университет «ЛЭТИ»',
   'ЛЭТИ',  'https://leti.ru/schedule/api')
ON CONFLICT (code) DO NOTHING;

-- STEP 2: Добавляем индексы GiST + BRIN для запросов к нескольким университетам

-- Добавляем композитный GiST для запросов availability_engine
CREATE INDEX IF NOT EXISTS idx_schedules_univ_room_tsrange
  ON schedules_import USING GIST (
    room_id,
    tstzrange(starts_at, ends_at)
  );

-- Индекс BRIN для пакетного импорта на starts_at
CREATE INDEX IF NOT EXISTS idx_schedules_brin_starts
  ON schedules_import USING BRIN (starts_at)
  WITH (pages_per_range = 128);

-- Частичный индекс: только подтвержденные бронирования
CREATE INDEX IF NOT EXISTS idx_bookings_confirmed_room_time
  ON bookings (room_id, starts_at, ends_at)
  WHERE status = 'confirmed';

-- Индекс для поиска корпусов по университету
CREATE INDEX IF NOT EXISTS idx_buildings_university
  ON buildings (university_id)
  WHERE university_id IS NOT NULL;

-- STEP 4: Добавление отслеживания источника schedules_import

-- Добавляем колонки для отслеживания данных в schedules_import
ALTER TABLE schedules_import
  ADD COLUMN IF NOT EXISTS university_id UUID REFERENCES universities(id) ON DELETE CASCADE,
  ADD COLUMN IF NOT EXISTS imported_at TIMESTAMPTZ DEFAULT NOW(),
  ADD COLUMN IF NOT EXISTS api_version VARCHAR(10) DEFAULT 'v1';

-- STEP 5: Создание представления для проверки доступности по всем университетам

-- Создаем view для availability engine
CREATE OR REPLACE VIEW v_room_availability AS
SELECT
  r.id            AS room_id,
  r.capacity,
  r.features,
  b.name          AS building_name,
  b.address,
  u.code          AS university_code,
  u.short_name    AS university_name,
  COUNT(bk.id)    AS active_bookings_count
FROM rooms r
JOIN buildings b ON r.building_id = b.id
JOIN universities u ON b.university_id = u.id
LEFT JOIN bookings bk
  ON bk.room_id = r.id
  AND bk.status = 'confirmed'
  AND bk.ends_at > NOW()
WHERE u.is_active = true
GROUP BY r.id, r.capacity, r.features,
         b.name, b.address, u.code, u.short_name;
