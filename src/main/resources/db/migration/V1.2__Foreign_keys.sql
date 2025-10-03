--------------------------------------------
------ Foreign keys required for FaCT ------
--------------------------------------------

-- COURT LOCAL AUTHORITIES
ALTER TABLE court_local_authorities
ADD CONSTRAINT fk_court_local_authorities_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

ALTER TABLE court_local_authorities
ADD CONSTRAINT fk_court_local_authorities_area_of_law
  FOREIGN KEY (area_of_law_id)
  REFERENCES area_of_law_types(id)
  ON DELETE SET NULL;

-- SERVICE AREA
ALTER TABLE service_area
ADD CONSTRAINT fk_service_area_area_of_law
  FOREIGN KEY (area_of_law_id)
  REFERENCES area_of_law_types(id)
  ON DELETE SET NULL;

-- COURT SERVICE AREAS
ALTER TABLE court_service_areas
ADD CONSTRAINT fk_court_service_areas_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

-- AUDIT
ALTER TABLE audit
ADD CONSTRAINT fk_audit_user
  FOREIGN KEY (user_id)
  REFERENCES users(id)
  ON DELETE SET NULL;

ALTER TABLE audit
ADD CONSTRAINT fk_audit_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

-- COURT LOCK
ALTER TABLE court_lock
ADD CONSTRAINT fk_court_lock_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

ALTER TABLE court_lock
ADD CONSTRAINT fk_court_lock_user
  FOREIGN KEY (user_id)
  REFERENCES users(id)
  ON DELETE CASCADE;

-- COURT ACCESSIBILITY OPTIONS
ALTER TABLE court_accessibility_options
ADD CONSTRAINT fk_court_accessibility_options_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

-- COURT FACILITIES
ALTER TABLE court_facilities
ADD CONSTRAINT fk_court_facilities_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

-- COURT TRANSLATION
ALTER TABLE court_translation
ADD CONSTRAINT fk_court_translation_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

-- COURT POSTCODES
ALTER TABLE court_postcodes
ADD CONSTRAINT fk_court_postcodes_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

-- COURT CODES
ALTER TABLE court_codes
ADD CONSTRAINT fk_court_codes_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

-- COURT AREAS OF LAW
ALTER TABLE court_areas_of_law
ADD CONSTRAINT fk_court_areas_of_law_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

-- COURT SINGLE POINTS OF ENTRY
ALTER TABLE court_single_points_of_entry
ADD CONSTRAINT fk_court_spe_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

-- COURT CONTACT DETAILS
ALTER TABLE court_contact_details
ADD CONSTRAINT fk_court_contact_details_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

ALTER TABLE court_contact_details
ADD CONSTRAINT fk_court_contact_details_description
  FOREIGN KEY (court_contact_description_id)
  REFERENCES contact_description_types(id)
  ON DELETE SET NULL;

-- COURT PROFESSIONAL INFORMATION
ALTER TABLE court_professional_information
ADD CONSTRAINT fk_court_professional_information_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

-- COURT DX CODES
ALTER TABLE court_dxcodes
ADD CONSTRAINT fk_court_dxcodes_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

-- COURT FAX
ALTER TABLE court_fax
ADD CONSTRAINT fk_court_fax_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

-- COURT PHOTO
ALTER TABLE court_photo
ADD CONSTRAINT fk_court_photo_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

-- COURT ADDRESS
ALTER TABLE court_address
ADD CONSTRAINT fk_court_address_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

-- COURT OPENING TIMES
ALTER TABLE court_opening_times
ADD CONSTRAINT fk_court_opening_times_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;

ALTER TABLE court_opening_times
ADD CONSTRAINT fk_court_opening_times_type
  FOREIGN KEY (opening_hour_type)
  REFERENCES opening_hour_types(id)
  ON DELETE SET NULL;

-- COURT COUNTER SERVICE OPENING HOURS
ALTER TABLE court_counter_service_opening_hours
ADD CONSTRAINT fk_court_counter_service_opening_hours_court
  FOREIGN KEY (court_id)
  REFERENCES court(id)
  ON DELETE CASCADE;


