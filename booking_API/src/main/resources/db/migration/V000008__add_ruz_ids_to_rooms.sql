-- V000008__add_ruz_ids_to_rooms.sql
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS ruz_id INTEGER;
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS building_ruz_id INTEGER;

-- Также добавим индекс для быстрого поиска
CREATE INDEX IF NOT EXISTS idx_rooms_ruz_id ON rooms(ruz_id);
