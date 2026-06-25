ALTER TABLE audit
  RENAME COLUMN court_id TO subject_id;

ALTER TABLE audit
  ADD COLUMN subject_type VARCHAR;

UPDATE audit
SET subject_type = 'COURT'
WHERE subject_type IS NULL;

ALTER TABLE audit
  ALTER COLUMN subject_type SET NOT NULL;
