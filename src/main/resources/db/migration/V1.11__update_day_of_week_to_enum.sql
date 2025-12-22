-- Change day_of_week from integer to enum type in court_opening_hours table

-- Step 1: Add a temporary column to hold the converted enum values
ALTER TABLE court_opening_hours ADD COLUMN day_of_week_temp VARCHAR(20);

-- Step 2: Migrate existing integer values to enum string values
UPDATE court_opening_hours
SET day_of_week_temp = CASE day_of_week
                         WHEN 0 THEN 'MONDAY'
                         WHEN 1 THEN 'TUESDAY'
                         WHEN 2 THEN 'WEDNESDAY'
                         WHEN 3 THEN 'THURSDAY'
                         WHEN 4 THEN 'FRIDAY'
                         WHEN 5 THEN 'SATURDAY'
                         WHEN 6 THEN 'SUNDAY'
                         WHEN 7 THEN 'EVERYDAY'
                         ELSE NULL
  END;

-- Step 3: Drop the old integer column
ALTER TABLE court_opening_hours DROP COLUMN day_of_week;

-- Step 4: Rename the temporary column to the original column name
ALTER TABLE court_opening_hours RENAME COLUMN day_of_week_temp TO day_of_week;

-- Step 5: Add a check constraint to ensure only valid enum values are stored
ALTER TABLE court_opening_hours
  ADD CONSTRAINT day_of_week_check
    CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY', 'EVERYDAY'));

-- Repeat steps for court_counter_service_opening_hours table

-- Step 1: Add a temporary column to hold the converted enum values
ALTER TABLE court_counter_service_opening_hours ADD COLUMN day_of_week_temp VARCHAR(20);

-- Step 2: Migrate existing integer values to enum string values
UPDATE court_counter_service_opening_hours
SET day_of_week_temp = CASE day_of_week
                         WHEN 0 THEN 'MONDAY'
                         WHEN 1 THEN 'TUESDAY'
                         WHEN 2 THEN 'WEDNESDAY'
                         WHEN 3 THEN 'THURSDAY'
                         WHEN 4 THEN 'FRIDAY'
                         WHEN 5 THEN 'SATURDAY'
                         WHEN 6 THEN 'SUNDAY'
                         WHEN 7 THEN 'EVERYDAY'
                         ELSE NULL
  END;

-- Step 3: Drop the old integer column
ALTER TABLE court_counter_service_opening_hours DROP COLUMN day_of_week;

-- Step 4: Rename the temporary column to the original column name
ALTER TABLE court_counter_service_opening_hours RENAME COLUMN day_of_week_temp TO day_of_week;

-- Step 5: Add a check constraint to ensure only valid enum values are stored
ALTER TABLE court_counter_service_opening_hours
  ADD CONSTRAINT counter_day_of_week_check
    CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY', 'EVERYDAY'));
