-- Migration: Update floor and capacity for existing rooms using heuristic analysis
-- This script applies FloorExtractor and CapacityEstimator logic at SQL level

-- Step 1: Update floor based on room name patterns
-- Pattern 1: Pure numeric (e.g., "314" -> 3, "1002" -> 10)
UPDATE rooms
SET floor = CASE
    -- 1-2 digits: first digit
    WHEN name ~ '^\d{1,2}$' THEN 
        CAST(SUBSTRING(name FROM 1 FOR 1) AS INTEGER)
    -- 3 digits: first digit
    WHEN name ~ '^\d{3}$' THEN 
        CAST(SUBSTRING(name FROM 1 FOR 1) AS INTEGER)
    -- 4+ digits: first 2 digits
    WHEN name ~ '^\d{4,}$' THEN 
        CAST(SUBSTRING(name FROM 1 FOR 2) AS INTEGER)
    -- Prefix + numeric (e.g., "Г-305" -> 3)
    WHEN name ~ '^[А-Яа-яA-Za-z]+-?\d+$' THEN
        CASE
            WHEN LENGTH(REGEXP_REPLACE(name, '[^0-9]', '', 'g')) <= 2 THEN
                CAST(SUBSTRING(REGEXP_REPLACE(name, '[^0-9]', '', 'g') FROM 1 FOR 1) AS INTEGER)
            WHEN LENGTH(REGEXP_REPLACE(name, '[^0-9]', '', 'g')) = 3 THEN
                CAST(SUBSTRING(REGEXP_REPLACE(name, '[^0-9]', '', 'g') FROM 1 FOR 1) AS INTEGER)
            ELSE
                CAST(SUBSTRING(REGEXP_REPLACE(name, '[^0-9]', '', 'g') FROM 1 FOR 2) AS INTEGER)
        END
    -- Numeric + suffix (e.g., "314А" -> 3)
    WHEN name ~ '^\d+[А-Яа-яA-Za-z]+$' THEN
        CASE
            WHEN LENGTH(REGEXP_REPLACE(name, '[^0-9]', '', 'g')) <= 2 THEN
                CAST(SUBSTRING(REGEXP_REPLACE(name, '[^0-9]', '', 'g') FROM 1 FOR 1) AS INTEGER)
            WHEN LENGTH(REGEXP_REPLACE(name, '[^0-9]', '', 'g')) = 3 THEN
                CAST(SUBSTRING(REGEXP_REPLACE(name, '[^0-9]', '', 'g') FROM 1 FOR 1) AS INTEGER)
            ELSE
                CAST(SUBSTRING(REGEXP_REPLACE(name, '[^0-9]', '', 'g') FROM 1 FOR 2) AS INTEGER)
        END
    ELSE NULL
END
WHERE floor IS NULL;

-- Step 2: Update capacity based on room name patterns
UPDATE rooms
SET capacity = CASE
    -- Large lecture halls (contains "актовый", "зал", "аудитория", "конференц")
    WHEN LOWER(name) ~ '.*(актовый|зал|аудитория|конференц).*' THEN 100
    -- Computer labs (contains "комп", "лаб", "lab", "пк")
    WHEN LOWER(name) ~ '.*(комп|лаб|lab|пк|компьютер).*' THEN 20
    -- Small rooms (1-2 digit room number)
    WHEN name ~ '^\d{1,2}$' THEN 15
    -- Large rooms (4+ digit room number)
    WHEN name ~ '^\d{4,}$' THEN 100
    -- Default
    ELSE 30
END
WHERE capacity = 30; -- Only update hardcoded default values

-- Create index for floor-based queries
CREATE INDEX IF NOT EXISTS idx_rooms_floor_name ON rooms(floor, name);

-- Verify results
-- SELECT 
--     university_code,
--     COUNT(*) as total_rooms,
--     COUNT(floor) as rooms_with_floor,
--     AVG(capacity) as avg_capacity,
--     MIN(capacity) as min_capacity,
--     MAX(capacity) as max_capacity
-- FROM rooms
-- GROUP BY university_code;
