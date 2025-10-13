-- Add explicit fk constraint between court_photo and user for user ids
ALTER TABLE court_photo
  ADD CONSTRAINT fk_court_photo_user
    FOREIGN KEY (updated_by_user_id)
      REFERENCES users (id)
      ON DELETE SET NULL;

-- Add NOT NULL constraint to file_link column in court_photo table
ALTER TABLE court_photo
  ALTER COLUMN file_link SET NOT NULL;

-- Add NOT NULL constraint to postcode column court_postcodes tableØ
ALTER TABLE court_postcodes
  ALTER COLUMN postcode SET NOT NULL;
