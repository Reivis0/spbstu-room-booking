-- Migration: V000013__auto_populate_rooms_from_import
-- Description: REMOVED. Logic moved to ruz-importer (UPSERT) to prevent race conditions.
-- Author: team

-- This migration is intentionally left empty.
-- The logic of building/room creation is now handled by ruz-importer service
-- via stored procedures or direct UPSERT statements after processing JSON.
