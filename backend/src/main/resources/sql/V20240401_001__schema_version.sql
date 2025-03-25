-- Migration: V20240401_001__schema_version.sql
-- Description: Creates the schema_version table to track executed migrations
-- Author: Development Team
-- Date: April 1, 2024
--
-- This script creates the schema_version table which serves as the foundation for
-- database versioning, storing information about which migrations have been applied,
-- when they were executed, and by whom. This table is essential for maintaining
-- database schema consistency across environments and providing a reliable audit
-- trail of all schema changes.

-- Begin transaction for atomic execution
BEGIN;

-- Create schema_version table to track migration history
CREATE TABLE IF NOT EXISTS schema_version (
    id SERIAL PRIMARY KEY,
    version VARCHAR(128) NOT NULL,
    description VARCHAR(255) NOT NULL,
    applied_at TIMESTAMP NOT NULL DEFAULT NOW(),
    applied_by VARCHAR(128) NOT NULL DEFAULT current_user,
    execution_time INT,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Ensure version uniqueness to prevent duplicate migrations
    CONSTRAINT schema_version_version_unique UNIQUE (version)
);

-- Add index for efficient version lookup
CREATE INDEX IF NOT EXISTS idx_schema_version_version ON schema_version(version);

-- Add index for chronological queries
CREATE INDEX IF NOT EXISTS idx_schema_version_applied_at ON schema_version(applied_at);

-- Add comments for documentation
COMMENT ON TABLE schema_version IS 'Tracks database migration script execution history';
COMMENT ON COLUMN schema_version.id IS 'Auto-incrementing identifier';
COMMENT ON COLUMN schema_version.version IS 'Migration version identifier (e.g., V20240401_001)';
COMMENT ON COLUMN schema_version.description IS 'Brief description of the migration';
COMMENT ON COLUMN schema_version.applied_at IS 'Timestamp when migration was executed';
COMMENT ON COLUMN schema_version.applied_by IS 'Database user who executed the migration';
COMMENT ON COLUMN schema_version.execution_time IS 'Migration execution time in milliseconds';
COMMENT ON COLUMN schema_version.success IS 'Indicates whether migration completed successfully';

-- Record this migration in the schema_version table itself
INSERT INTO schema_version (version, description, applied_at, applied_by, execution_time, success)
VALUES ('V20240401_001', 'Create schema_version table', NOW(), current_user, 0, TRUE);

-- Commit transaction
COMMIT;