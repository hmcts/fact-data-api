CREATE TABLE IF NOT EXISTS migration_audit (
    id UUID PRIMARY KEY NOT NULL,
    table_name VARCHAR NOT NULL,
    field_name VARCHAR NOT NULL,
    status VARCHAR NOT NULL,
    details VARCHAR,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
