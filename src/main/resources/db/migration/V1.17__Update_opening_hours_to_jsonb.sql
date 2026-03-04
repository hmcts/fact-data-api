ALTER TABLE court_opening_hours ADD COLUMN opening_times_details JSONB;
ALTER TABLE court_opening_hours DROP COLUMN day_of_week;
ALTER TABLE court_opening_hours DROP COLUMN opening_hour;
ALTER TABLE court_opening_hours DROP COLUMN closing_hour;

ALTER TABLE court_counter_service_opening_hours ADD COLUMN court_types UUID[];
ALTER TABLE court_counter_service_opening_hours ADD COLUMN opening_times_details JSONB;
ALTER TABLE court_counter_service_opening_hours DROP COLUMN day_of_week;
ALTER TABLE court_counter_service_opening_hours DROP COLUMN opening_hour;
ALTER TABLE court_counter_service_opening_hours DROP COLUMN closing_hour;
