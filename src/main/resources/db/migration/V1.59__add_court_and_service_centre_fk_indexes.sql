-- Speeds up the EAGER-fetched child-table lookups performed for every court and
-- service centre details request (e.g. GET /courts/{id}/v1, GET /courts/slug/{slug}/v1).
-- Postgres does not automatically index foreign-key columns.

-- COURT
CREATE INDEX court_address_court_id_idx ON court_address (court_id);
CREATE INDEX court_opening_hours_court_id_idx ON court_opening_hours (court_id);
CREATE INDEX court_counter_service_opening_hours_court_id_idx ON court_counter_service_opening_hours (court_id);
CREATE INDEX court_contact_details_court_id_idx ON court_contact_details (court_id);
CREATE INDEX court_translation_court_id_idx ON court_translation (court_id);
CREATE INDEX court_accessibility_options_court_id_idx ON court_accessibility_options (court_id);
CREATE INDEX court_facilities_court_id_idx ON court_facilities (court_id);
CREATE INDEX court_professional_information_court_id_idx ON court_professional_information (court_id);
CREATE INDEX court_areas_of_law_court_id_idx ON court_areas_of_law (court_id);
CREATE INDEX court_dxcodes_court_id_idx ON court_dxcodes (court_id);
CREATE INDEX court_codes_court_id_idx ON court_codes (court_id);
CREATE INDEX court_fax_court_id_idx ON court_fax (court_id);
CREATE INDEX court_photo_court_id_idx ON court_photo (court_id);
CREATE INDEX court_single_points_of_entry_court_id_idx ON court_single_points_of_entry (court_id);
CREATE INDEX court_local_authorities_court_id_idx ON court_local_authorities (court_id);

-- SERVICE CENTRE
CREATE INDEX service_centre_address_service_centre_id_idx ON service_centre_address (service_centre_id);
CREATE INDEX service_centre_contact_details_service_centre_id_idx
  ON service_centre_contact_details (service_centre_id);
CREATE INDEX service_centre_areas_of_law_service_centre_id_idx ON service_centre_areas_of_law (service_centre_id);
