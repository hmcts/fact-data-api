-- Temporary table that maps the legacy FaCT court identifier to the new UUID. Remove the table
-- once the production migration is complete.
CREATE TABLE legacy_court_mapping (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    court_id UUID NOT NULL REFERENCES court(id) ON DELETE CASCADE,
    legacy_court_id NUMERIC(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE legacy_court_mapping IS 'Temporary mapping between legacy FaCT IDs and new court UUIDs';
COMMENT ON COLUMN legacy_court_mapping.legacy_court_id IS 'Legacy FaCT identifier (remove after migration)';

INSERT INTO region (id, name, country)
SELECT gen_random_uuid(), 'Service Centre', 'England'
  WHERE NOT EXISTS (
    SELECT 1 FROM region WHERE name = 'Service Centre' AND country = 'England'
);

CREATE TABLE IF NOT EXISTS migration_audit (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  migration_name VARCHAR(100) NOT NULL UNIQUE,
  status VARCHAR(30) NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
