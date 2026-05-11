-- Flyway 10.x, PostgreSQL 16
-- Добавляем поля для хранения аудитории напрямую из RUZ API
-- и поля для хранения информации о предмете и преподавателе

-- ШАГ 1: Добавляем колонки (ОБЯЗАТЕЛЬНО ДО CREATE INDEX)
ALTER TABLE schedules_import
    ADD COLUMN IF NOT EXISTS ruz_room_id TEXT,
    ADD COLUMN IF NOT EXISTS room_name   TEXT,
    ADD COLUMN IF NOT EXISTS subject     TEXT,
    ADD COLUMN IF NOT EXISTS teacher     TEXT;

-- ШАГ 2: Делаем room_id опциональным
ALTER TABLE schedules_import
    ALTER COLUMN room_id DROP NOT NULL;

-- ШАГ 3: Индексы
CREATE INDEX IF NOT EXISTS idx_schedules_import_ruz_room
    ON schedules_import (university_id, ruz_room_id);

CREATE INDEX IF NOT EXISTS idx_schedules_import_room_name
    ON schedules_import (room_name);

-- ШАГ 4: Гарантируем уникальный индекс для ON CONFLICT
DROP INDEX IF EXISTS idx_dedup_university_hash;
CREATE UNIQUE INDEX IF NOT EXISTS idx_schedules_import_dedup 
    ON schedules_import (university_id, data_hash);
