-- Drop the existing timezone-unaware constraints
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS chk_working_hours_start;
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS chk_working_hours_end;

-- Delete rows that violate the new timezone-aware rules
DELETE FROM bookings
WHERE EXTRACT(HOUR FROM starts_at AT TIME ZONE 'Europe/Moscow') < 8
   OR EXTRACT(HOUR FROM ends_at AT TIME ZONE 'Europe/Moscow') > 21
   OR (EXTRACT(HOUR FROM ends_at AT TIME ZONE 'Europe/Moscow') = 21 AND EXTRACT(MINUTE FROM ends_at AT TIME ZONE 'Europe/Moscow') > 0);

-- Add correct timezone-aware constraints for working hours (08:00 - 21:00 MSK)
ALTER TABLE bookings
ADD CONSTRAINT chk_working_hours_start
CHECK (EXTRACT(HOUR FROM starts_at AT TIME ZONE 'Europe/Moscow') >= 8);

ALTER TABLE bookings
ADD CONSTRAINT chk_working_hours_end
CHECK (
    EXTRACT(HOUR FROM ends_at AT TIME ZONE 'Europe/Moscow') < 21
    OR (
        EXTRACT(HOUR FROM ends_at AT TIME ZONE 'Europe/Moscow') = 21 
        AND EXTRACT(MINUTE FROM ends_at AT TIME ZONE 'Europe/Moscow') = 0
    )
);
