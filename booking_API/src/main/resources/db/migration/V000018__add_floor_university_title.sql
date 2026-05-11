-- Add floor and university_code to rooms table
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS floor INTEGER;
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS university_code VARCHAR(50);

-- Add university_code to buildings table
ALTER TABLE buildings ADD COLUMN IF NOT EXISTS university_code VARCHAR(50);

-- Add title to bookings table
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS title VARCHAR(255);

-- Create indexes for new columns
CREATE INDEX IF NOT EXISTS idx_rooms_floor ON rooms(floor);
CREATE INDEX IF NOT EXISTS idx_rooms_university_code ON rooms(university_code);
CREATE INDEX IF NOT EXISTS idx_buildings_university_code ON buildings(university_code);
