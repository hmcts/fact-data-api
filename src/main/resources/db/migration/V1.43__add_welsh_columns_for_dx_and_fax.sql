ALTER TABLE court_dxcodes
    ADD COLUMN IF NOT EXISTS explanation_cy VARCHAR(250);

ALTER TABLE court_fax
    ADD COLUMN IF NOT EXISTS description_cy VARCHAR(250);

