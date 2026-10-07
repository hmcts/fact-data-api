-- Speeds up paged audit queries filtered by subject (id + type) and created_at,
-- and the join to users for email-based filtering.
CREATE INDEX audit_subject_id_subject_type_created_at_idx
  ON audit (subject_id, subject_type, created_at);

CREATE INDEX audit_user_id_idx
  ON audit (user_id);
