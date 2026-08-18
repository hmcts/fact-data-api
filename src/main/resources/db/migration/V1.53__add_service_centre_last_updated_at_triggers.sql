-- Add a common trigger function
CREATE OR REPLACE FUNCTION update_service_centre_last_updated_at_change() RETURNS trigger AS $$
BEGIN
  UPDATE service_centre
  SET last_updated_at = NOW()
  WHERE id = COALESCE(NEW.service_centre_id, OLD.service_centre_id);
  RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Apply the trigger to all currently relevant service centre "child" tables
CREATE TRIGGER service_centre_address_service_centre_update_last_updated_at_trg
  AFTER INSERT OR UPDATE OR DELETE ON service_centre_address
  FOR EACH ROW EXECUTE FUNCTION update_service_centre_last_updated_at_change();

CREATE TRIGGER service_centre_areas_of_law_service_centre_update_last_updated_at_trg
  AFTER INSERT OR UPDATE OR DELETE ON service_centre_areas_of_law
  FOR EACH ROW EXECUTE FUNCTION update_service_centre_last_updated_at_change();

CREATE TRIGGER service_centre_contact_details_service_centre_update_last_updated_at_trg
  AFTER INSERT OR UPDATE OR DELETE ON service_centre_contact_details
  FOR EACH ROW EXECUTE FUNCTION update_service_centre_last_updated_at_change();
