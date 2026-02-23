-- remove the current constraints from the audit table to allow
-- audit records to persist after deletion of the entities they
-- are linked to
ALTER TABLE audit
  DROP CONSTRAINT fk_audit_user;

ALTER TABLE audit
  DROP CONSTRAINT fk_audit_court;

-- replace the before and after action_data columns with a single
-- action_data_diff column
ALTER TABLE audit
  DROP COLUMN action_data_before;

ALTER TABLE audit
  RENAME COLUMN action_data_after to action_data_diff;

-- Add and entity column to allow context for an audit action
ALTER TABLE audit
  ADD COLUMN action_entity VARCHAR;
