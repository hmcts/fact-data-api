-- remove the foreign key constraint on court_lock
ALTER TABLE court_lock
  DROP CONSTRAINT fk_court_lock_court;

-- rename court_lock to lock and add subject_type column to generalise
-- its usage for both courts and service centres
ALTER TABLE court_lock RENAME TO lock;

ALTER TABLE lock
  RENAME COLUMN court_id TO subject_id;

ALTER TABLE lock
  ADD COLUMN subject_type VARCHAR;

UPDATE lock
SET subject_type = 'COURT'
WHERE subject_type IS NULL;

ALTER TABLE lock
  ALTER COLUMN subject_type SET NOT NULL;
