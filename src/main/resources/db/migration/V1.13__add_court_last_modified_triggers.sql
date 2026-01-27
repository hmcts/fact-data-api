-- Add a common trigger function
CREATE OR REPLACE FUNCTION update_court_last_modified_on_change() RETURNS trigger AS $$
BEGIN
  UPDATE court
  SET last_updated_at = NOW()
  WHERE id = COALESCE(NEW.court_id, OLD.court_id);
  RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Apply the trigger to all currently relevant "child" tables
CREATE TRIGGER court_accessibility_options_court_update_last_modified_trg
  AFTER INSERT OR UPDATE OR DELETE ON court_accessibility_options
  FOR EACH ROW EXECUTE FUNCTION update_court_last_modified_on_change();

CREATE TRIGGER court_address_court_update_last_modified_trg
  AFTER INSERT OR UPDATE OR DELETE ON court_address
  FOR EACH ROW EXECUTE FUNCTION update_court_last_modified_on_change();

CREATE TRIGGER court_areas_of_law_court_update_last_modified_trg
  AFTER INSERT OR UPDATE OR DELETE ON court_areas_of_law
  FOR EACH ROW EXECUTE FUNCTION update_court_last_modified_on_change();

CREATE TRIGGER court_codes_court_update_last_modified_trg
  AFTER INSERT OR UPDATE OR DELETE ON court_codes
  FOR EACH ROW EXECUTE FUNCTION update_court_last_modified_on_change();

CREATE TRIGGER court_contact_details_court_update_last_modified_trg
  AFTER INSERT OR UPDATE OR DELETE ON court_contact_details
  FOR EACH ROW EXECUTE FUNCTION update_court_last_modified_on_change();

CREATE TRIGGER court_counter_service_opening_hours_court_update_last_modified_trg
  AFTER INSERT OR UPDATE OR DELETE ON court_counter_service_opening_hours
  FOR EACH ROW EXECUTE FUNCTION update_court_last_modified_on_change();

CREATE TRIGGER court_dxcodes_court_update_last_modified_trg
  AFTER INSERT OR UPDATE OR DELETE ON court_dxcodes
  FOR EACH ROW EXECUTE FUNCTION update_court_last_modified_on_change();

CREATE TRIGGER court_facilities_court_update_last_modified_trg
  AFTER INSERT OR UPDATE OR DELETE ON court_facilities
  FOR EACH ROW EXECUTE FUNCTION update_court_last_modified_on_change();

CREATE TRIGGER court_fax_court_update_last_modified_trg
  AFTER INSERT OR UPDATE OR DELETE ON court_fax
  FOR EACH ROW EXECUTE FUNCTION update_court_last_modified_on_change();

CREATE TRIGGER court_local_authorities_court_update_last_modified_trg
  AFTER INSERT OR UPDATE OR DELETE ON court_local_authorities
  FOR EACH ROW EXECUTE FUNCTION update_court_last_modified_on_change();

CREATE TRIGGER court_opening_hours_court_update_last_modified_trg
  AFTER INSERT OR UPDATE OR DELETE ON court_opening_hours
  FOR EACH ROW EXECUTE FUNCTION update_court_last_modified_on_change();

CREATE TRIGGER court_photo_court_update_last_modified_trg
  AFTER INSERT OR UPDATE OR DELETE ON court_photo
  FOR EACH ROW EXECUTE FUNCTION update_court_last_modified_on_change();

CREATE TRIGGER court_postcodes_court_update_last_modified_trg
  AFTER INSERT OR UPDATE OR DELETE ON court_postcodes
  FOR EACH ROW EXECUTE FUNCTION update_court_last_modified_on_change();

CREATE TRIGGER court_professional_information_court_update_last_modified_trg
  AFTER INSERT OR UPDATE OR DELETE ON court_professional_information
  FOR EACH ROW EXECUTE FUNCTION update_court_last_modified_on_change();

CREATE TRIGGER court_service_areas_court_update_last_modified_trg
  AFTER INSERT OR UPDATE OR DELETE ON court_service_areas
  FOR EACH ROW EXECUTE FUNCTION update_court_last_modified_on_change();

CREATE TRIGGER court_single_points_of_entry_court_update_last_modified_trg
  AFTER INSERT OR UPDATE OR DELETE ON court_single_points_of_entry
  FOR EACH ROW EXECUTE FUNCTION update_court_last_modified_on_change();

CREATE TRIGGER court_translation_court_update_last_modified_trg
  AFTER INSERT OR UPDATE OR DELETE ON court_translation
  FOR EACH ROW EXECUTE FUNCTION update_court_last_modified_on_change();
