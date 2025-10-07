-- fix incorrect column name (county instead of country) in region table
ALTER TABLE region
RENAME COLUMN county to country;
