-- Migration: V000013__auto_populate_rooms_from_import
-- Description: Automatically populate rooms and buildings from schedules_import unique rooms
-- Author: team

-- Step 1: Create a default building for each university if none exists
INSERT INTO buildings (id, university_id, code, name, address)
SELECT 
    uuid_generate_v4(), 
    u.id, 
    'BUILDING_DEFAULT_' || u.id, 
    'Главный корпус (' || u.name || ')', 
    'Университетская наб., д.1'
FROM universities u
ON CONFLICT (code) DO NOTHING;

-- Step 2: Extract unique rooms from schedules_import and insert into rooms
-- Only for records where ruz_room_id or room_name is present
INSERT INTO rooms (id, building_id, university_id, ruz_id, name, code, capacity, features)
SELECT
    uuid_generate_v4(),
    b.id,
    b.university_id,
    NULL, -- ruz_id is integer in rooms, but ruz_room_id is text in schedules_import, so we leave it null for now
    sub.room_name,
    sub.room_name, -- use name as code for simplicity
    30, -- default capacity
    '{}'::jsonb
FROM (
    SELECT DISTINCT university_id, room_name
    FROM schedules_import
    WHERE room_name IS NOT NULL AND room_name != ''
) sub
JOIN buildings b ON b.university_id = sub.university_id AND b.code = 'BUILDING_DEFAULT_' || sub.university_id
ON CONFLICT DO NOTHING;

-- Step 3: Update schedules_import to link to the new rooms
-- This will enable the API to show room data for imported schedules
UPDATE schedules_import s
SET room_id = r.id
FROM rooms r
WHERE s.room_id IS NULL
  AND s.room_name = r.name
  AND s.university_id = r.university_id;
