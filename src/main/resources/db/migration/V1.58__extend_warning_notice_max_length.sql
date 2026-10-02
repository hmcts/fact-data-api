-- FACT-3113
--Extend the length of the warning notice fields in the court and service centre tables

ALTER TABLE court
    ALTER COLUMN warning_notice TYPE VARCHAR(700),
    ALTER COLUMN warning_notice_cy TYPE VARCHAR(700),

ALTER TABLE service_centre
    ALTER COLUMN warning_notice TYPE VARCHAR(700),
    ALTER COLUMN warning_notice_cy TYPE VARCHAR(700),
