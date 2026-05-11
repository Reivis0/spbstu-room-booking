-- Migration: V000014__sync_room_build
-- Description: Expand schedules_import with building info and add unique constraints for automated upsert
-- Author: team

-- Step 1: Add building related columns to schedules_import
ALTER TABLE schedules_import
    ADD COLUMN IF NOT EXISTS ruz_building_id   TEXT,
    ADD COLUMN IF NOT EXISTS building_name     TEXT,
    ADD COLUMN IF NOT EXISTS building_address  TEXT;

-- Step 2: Ensure tables rooms and buildings have ruz_id and university_id for unique identification
-- In V8 we added ruz_id to rooms as INTEGER. 
-- In schedules_import ruz_room_id is TEXT. 
-- For consistency with RUZ APIs, we should use TEXT or allow conversion.
-- LET'S MAKE ruz_id TEXT in both rooms and buildings to support all university types (Oid, string, int).

ALTER TABLE rooms ALTER COLUMN ruz_id TYPE TEXT;
ALTER TABLE buildings ADD COLUMN IF NOT EXISTS ruz_id TEXT;

-- Step 3: Add UNIQUE constraints for UPSERT support
-- Constraint for rooms
ALTER TABLE rooms
    DROP CONSTRAINT IF EXISTS rooms_ruz_id_university_unique;
ALTER TABLE rooms
    ADD CONSTRAINT rooms_ruz_id_university_unique
    UNIQUE (ruz_id, university_id);

-- Constraint for buildings
ALTER TABLE buildings
    DROP CONSTRAINT IF EXISTS buildings_ruz_id_university_unique;
ALTER TABLE buildings
    ADD CONSTRAINT buildings_ruz_id_university_unique
    UNIQUE (ruz_id, university_id);

-- Step 4: Add index for faster syncing
CREATE INDEX IF NOT EXISTS idx_schedules_import_ruz_building ON schedules_import (ruz_building_id);
