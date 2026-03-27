-- V000004__db_audit_fixes.sql

-- ❌ ПРОБЛЕМА: таблица bookings — отсутствует внешний ключ user_id с каскадным удалением (ON DELETE CASCADE)
-- ✅ РЕШЕНИЕ: добавляем внешний ключ с каскадным удалением
ALTER TABLE bookings
    ADD CONSTRAINT bookings_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- ❌ ПРОБЛЕМА: таблица bookings — нет колонки version для оптимистичной блокировки
-- ✅ РЕШЕНИЕ: добавляем колонку version со значением по умолчанию
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 1;

-- ❌ ПРОБЛЕМА: таблица rooms — нет колонки updated_at
-- ✅ РЕШЕНИЕ: добавляем колонку updated_at
ALTER TABLE rooms
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- ❌ ПРОБЛЕМА: таблица buildings — нет колонки updated_at
-- ✅ РЕШЕНИЕ: добавляем колонку updated_at
ALTER TABLE buildings
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- ❌ ПРОБЛЕМА: таблица outbox_events — отсутствует полностью (нужна для паттерна Outbox)
-- ✅ РЕШЕНИЕ: создаем таблицу outbox_events
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type TEXT NOT NULL,
    payload JSONB NOT NULL,
    published BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ❌ ПРОБЛЕМА: отсутствуют необходимые индексы для часто выполняемых запросов (hot query paths)
-- ✅ РЕШЕНИЕ: добавляем индексы GIST, BRIN, B-tree

-- Поиск бронирований по аудитории + временному диапазону (самый критичный запрос)
CREATE INDEX IF NOT EXISTS idx_bookings_room_tsrange
  ON bookings USING GIST (room_id, tstzrange(starts_at, ends_at));

-- Только активные бронирования (частичный индекс, статус confirmed)
CREATE INDEX IF NOT EXISTS idx_bookings_confirmed
  ON bookings (room_id, starts_at, ends_at)
  WHERE status = 'confirmed';

-- Опрос очереди outbox (неопубликованные события)
CREATE INDEX IF NOT EXISTS idx_outbox_unpublished
  ON outbox_events (created_at)
  WHERE published = false;

-- Дедупликация расписаний при импорте
CREATE UNIQUE INDEX IF NOT EXISTS idx_schedules_hash
  ON schedules_import (hash);

-- Поиск пользователя для аутентификации (только активные пользователи)
CREATE INDEX IF NOT EXISTS idx_users_email
  ON users (email)
  WHERE is_active = true;

-- BRIN для сканирования диапазонов при массовом импорте (эффективно для отсортированных данных)
CREATE INDEX IF NOT EXISTS idx_schedules_brin_updated
  ON schedules_import USING BRIN (starts_at);

-- ❌ ПРОБЛЕМА: отсутствует триггер для автоматического обновления updated_at
-- ✅ РЕШЕНИЕ: создаем функцию set_updated_at() и применяем триггеры

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_bookings_updated_at ON bookings;
CREATE TRIGGER trg_bookings_updated_at
  BEFORE UPDATE ON bookings
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_rooms_updated_at ON rooms;
CREATE TRIGGER trg_rooms_updated_at
  BEFORE UPDATE ON rooms
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at
  BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_buildings_updated_at ON buildings;
CREATE TRIGGER trg_buildings_updated_at
  BEFORE UPDATE ON buildings
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_schedules_import_updated_at ON schedules_import;
CREATE TRIGGER trg_schedules_import_updated_at
  BEFORE UPDATE ON schedules_import
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
