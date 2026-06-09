ALTER TABLE local_authority_types
  ADD COLUMN is_parent BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE local_authority_types lat
SET is_parent = NOT EXISTS (
  SELECT 1
  FROM local_authority_types parent
  WHERE lat.custodian_code = ANY(parent.child_custodian_codes)
);
