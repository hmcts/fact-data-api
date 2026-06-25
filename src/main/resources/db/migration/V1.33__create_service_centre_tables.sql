CREATE TABLE service_centre (
  id UUID PRIMARY KEY NOT NULL,
  name VARCHAR NOT NULL,
  slug VARCHAR,
  open BOOLEAN,
  warning_notice VARCHAR,
  created_at TIMESTAMP NOT NULL,
  last_updated_at TIMESTAMP NOT NULL,
  service_area_ids UUID[],
  catchment_type VARCHAR
);

CREATE TABLE service_centre_address (
  id UUID PRIMARY KEY NOT NULL,
  service_centre_id UUID NOT NULL,
  address_line_1 VARCHAR,
  address_line_2 VARCHAR,
  town_city VARCHAR,
  county VARCHAR,
  postcode VARCHAR,
  lat DECIMAL(9,6),
  lon DECIMAL(9,6),
  address_type VARCHAR
);

CREATE TABLE service_centre_contact_details (
  id UUID PRIMARY KEY NOT NULL,
  service_centre_id UUID NOT NULL,
  service_centre_contact_description_id UUID,
  explanation VARCHAR,
  explanation_cy VARCHAR,
  email VARCHAR,
  phone_number VARCHAR
);

CREATE TABLE service_centre_areas_of_law (
  id UUID PRIMARY KEY NOT NULL,
  service_centre_id UUID NOT NULL,
  areas_of_law UUID[]
);

ALTER TABLE service_centre_address
ADD CONSTRAINT fk_service_centre_address_service_centre
  FOREIGN KEY (service_centre_id)
  REFERENCES service_centre(id)
  ON DELETE CASCADE;

ALTER TABLE service_centre_contact_details
ADD CONSTRAINT fk_service_centre_contact_details_service_centre
  FOREIGN KEY (service_centre_id)
  REFERENCES service_centre(id)
  ON DELETE CASCADE;

ALTER TABLE service_centre_contact_details
ADD CONSTRAINT fk_service_centre_contact_details_description
  FOREIGN KEY (service_centre_contact_description_id)
  REFERENCES contact_description_types(id)
  ON DELETE SET NULL;

ALTER TABLE service_centre_areas_of_law
ADD CONSTRAINT fk_service_centre_areas_of_law_service_centre
  FOREIGN KEY (service_centre_id)
  REFERENCES service_centre(id)
  ON DELETE CASCADE;
