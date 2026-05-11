-- Unique index for deduplication: one hash per university
CREATE UNIQUE INDEX IF NOT EXISTS idx_dedup_university_hash 
ON schedules_import(university_id, data_hash) 
WHERE data_hash IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_schedules_date 
ON schedules_import(university_id, starts_at);
