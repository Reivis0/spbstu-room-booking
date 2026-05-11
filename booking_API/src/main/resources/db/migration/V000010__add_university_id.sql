-- Migration: V000010__add_university_id
-- Description: Add missing columns for ruz-importer deduplication
-- Author: team

-- Additional fields for deduplication in schedules_import
-- university_id is already added in V5 for all tables.

ALTER TABLE schedules_import ADD COLUMN IF NOT EXISTS data_hash VARCHAR(64);
ALTER TABLE schedules_import ADD COLUMN IF NOT EXISTS raw_source JSONB;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_schedules_hash ON schedules_import(data_hash);
