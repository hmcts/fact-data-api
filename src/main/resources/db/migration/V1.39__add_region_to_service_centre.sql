ALTER TABLE service_centre
  ADD COLUMN region_id UUID;

ALTER TABLE service_centre
  ADD CONSTRAINT fk_service_centre_region
  FOREIGN KEY (region_id)
  REFERENCES region(id);
