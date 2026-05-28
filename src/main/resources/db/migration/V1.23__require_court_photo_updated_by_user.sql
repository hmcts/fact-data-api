ALTER TABLE court_photo
  DROP CONSTRAINT IF EXISTS fk_court_photo_user;

ALTER TABLE court_photo
  ADD CONSTRAINT fk_court_photo_user
    FOREIGN KEY (updated_by_user_id)
      REFERENCES users (id);

ALTER TABLE court_photo
  ADD CONSTRAINT ck_court_photo_updated_by_user_id_not_null
    CHECK (updated_by_user_id IS NOT NULL) NOT VALID;
