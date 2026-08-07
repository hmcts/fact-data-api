-- Ensure the FK allows deleting users without removing court_photo rows.
ALTER TABLE court_photo
  DROP CONSTRAINT IF EXISTS fk_court_photo_user;

ALTER TABLE court_photo
DROP CONSTRAINT IF EXISTS ck_court_photo_updated_by_user_id_not_null;

ALTER TABLE court_photo
  ADD CONSTRAINT fk_court_photo_user
    FOREIGN KEY (updated_by_user_id)
      REFERENCES users (id)
      ON DELETE SET NULL;
