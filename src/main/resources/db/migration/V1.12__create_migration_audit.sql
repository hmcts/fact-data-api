CREATE TABLE IF NOT EXISTS migration_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    migration_name VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
