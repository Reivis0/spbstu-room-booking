CREATE TABLE IF NOT EXISTS schema_migrations (
    id             SERIAL PRIMARY KEY,
    version        VARCHAR(50)  NOT NULL UNIQUE,
    name           VARCHAR(255) NOT NULL,
    applied_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    rolled_back_at TIMESTAMPTZ,
    status         VARCHAR(20)  NOT NULL DEFAULT 'applied'
                  CHECK (status IN ('applied', 'rolled_back', 'failed')),
    checksum       VARCHAR(64)  NOT NULL,
    applied_by     VARCHAR(100) NOT NULL DEFAULT current_user,
    duration_ms    INTEGER
);

CREATE INDEX idx_schema_migrations_version ON schema_migrations(version);
CREATE INDEX idx_schema_migrations_status  ON schema_migrations(status);
