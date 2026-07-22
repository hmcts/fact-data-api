UPDATE users
SET favourite_courts = ARRAY[]::UUID[];

ALTER TABLE users
  ALTER COLUMN favourite_courts SET DEFAULT ARRAY[]::UUID[],
  ALTER COLUMN favourite_courts SET NOT NULL,
  ADD COLUMN favourite_service_centres UUID[] NOT NULL DEFAULT ARRAY[]::UUID[];

CREATE INDEX users_favourite_courts_idx
  ON users USING GIN (favourite_courts);

CREATE INDEX users_favourite_service_centres_idx
  ON users USING GIN (favourite_service_centres);
