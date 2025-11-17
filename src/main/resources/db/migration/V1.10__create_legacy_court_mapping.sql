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
