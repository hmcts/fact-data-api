CREATE TEMP TABLE aggregated_opening_hours AS
SELECT
  gen_random_uuid() as id,
  court_id,
  opening_hour_type,
  jsonb_agg(
    jsonb_build_object(
      'dayOfWeek', day_of_week,
      'openingTime', TO_CHAR(opening_hour, 'HH24:MI:SS'),
      'closingTime', TO_CHAR(closing_hour, 'HH24:MI:SS')
    ) ORDER BY day_of_week
  ) as opening_times_details
FROM court_opening_hours
GROUP BY court_id, opening_hour_type;

TRUNCATE TABLE court_opening_hours;

ALTER TABLE court_opening_hours ADD COLUMN opening_times_details JSONB;
ALTER TABLE court_opening_hours DROP COLUMN day_of_week;
ALTER TABLE court_opening_hours DROP COLUMN opening_hour;
ALTER TABLE court_opening_hours DROP COLUMN closing_hour;

INSERT INTO court_opening_hours (id, court_id, opening_hour_type, opening_times_details)
SELECT id, court_id, opening_hour_type, opening_times_details
FROM aggregated_opening_hours;

DROP TABLE aggregated_opening_hours;
