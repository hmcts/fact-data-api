CREATE TABLE approvals (
  id UUID PRIMARY KEY NOT NULL,
  subject_id UUID NOT NULL,
  subject_type VARCHAR NOT NULL,
  user_id UUID NOT NULL,
  last_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE approvals
  ADD CONSTRAINT fk_approvals_user
    FOREIGN KEY (user_id)
      REFERENCES users(id)
      ON DELETE CASCADE;

ALTER TABLE approvals
  ADD CONSTRAINT uk_approvals_subject
    UNIQUE (subject_id, subject_type);
