-- Guarantee only one lock can ever exist for subject_id/subject_type page combination
ALTER TABLE lock
  ADD CONSTRAINT unique_lock_subject_page UNIQUE (subject_id, subject_type, page);
