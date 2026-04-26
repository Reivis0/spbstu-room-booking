-- Add CHECK constraint to enforce working hours (08:00 - 21:00)
-- This ensures database-level validation that bookings cannot be created outside working hours

-- First, delete any existing bookings that violate working hours
DELETE FROM bookings 
WHERE EXTRACT(HOUR FROM starts_at AT TIME ZONE 'UTC') < 8 
   OR EXTRACT(HOUR FROM ends_at AT TIME ZONE 'UTC') > 21;

ALTER TABLE bookings 
ADD CONSTRAINT chk_working_hours_start 
CHECK (EXTRACT(HOUR FROM starts_at AT TIME ZONE 'UTC') >= 8);

ALTER TABLE bookings 
ADD CONSTRAINT chk_working_hours_end 
CHECK (EXTRACT(HOUR FROM ends_at AT TIME ZONE 'UTC') < 21 
       OR (EXTRACT(HOUR FROM ends_at AT TIME ZONE 'UTC') = 21 AND EXTRACT(MINUTE FROM ends_at AT TIME ZONE 'UTC') = 0));

-- Create index for performance on time-based queries
CREATE INDEX IF NOT EXISTS idx_bookings_time_range ON bookings(starts_at, ends_at);
