--------------------------------------------
--------- Tables required for FaCT ---------
--------------------------------------------

-- USERS
CREATE TABLE users (
  id UUID PRIMARY KEY NOT NULL,
  email VARCHAR NOT NULL,
  sso_id UUID NOT NULL,
  favourite_courts UUID[],
  last_login TIMESTAMP
);

-- LOCAL AUTHORITY TYPES
CREATE TABLE local_authority_types (
  id UUID PRIMARY KEY NOT NULL,
  name VARCHAR NOT NULL
);

-- COURT LOCAL AUTHORITIES
CREATE TABLE court_local_authorities (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  area_of_law_id UUID,
  local_authority_ids UUID[]
);

-- SERVICE AREA
CREATE TABLE service_area (
  id UUID PRIMARY KEY NOT NULL,
  name VARCHAR NOT NULL,
  name_cy VARCHAR NOT NULL,
  description VARCHAR,
  description_cy VARCHAR,
  online_url VARCHAR,
  online_text VARCHAR,
  online_text_cy VARCHAR,
  text VARCHAR,
  text_cy VARCHAR,
  catchment_method VARCHAR,
  area_of_law_id UUID,
  type VARCHAR,
  sort_order INTEGER
);

-- COURT SERVICE AREAS
CREATE TABLE court_service_areas (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  service_area_id UUID[],
  catchment_type VARCHAR
);

-- SERVICE
CREATE TABLE service (
  id UUID PRIMARY KEY NOT NULL,
  name VARCHAR NOT NULL,
  name_cy VARCHAR NOT NULL,
  description VARCHAR,
  description_cy VARCHAR,
  service_areas UUID[]
);

-- AUDIT
CREATE TABLE audit (
  id UUID PRIMARY KEY NOT NULL,
  user_id UUID NOT NULL,
  action_type VARCHAR,
  action_data_before JSONB,
  action_data_after JSONB,
  created_at TIMESTAMP,
  court_id UUID NOT NULL
);

-- CONTACT DESCRIPTION TYPES
CREATE TABLE contact_description_types (
  id UUID PRIMARY KEY NOT NULL,
  name VARCHAR NOT NULL,
  name_cy VARCHAR NOT NULL
);

-- COURT LOCK
CREATE TABLE court_lock (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  user_id UUID NOT NULL,
  page VARCHAR,
  lock_acquired TIMESTAMP
);

-- COURT ACCESSIBILITY OPTIONS
CREATE TABLE court_accessibility_options (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  accessible_parking BOOLEAN,
  accessible_parking_phone_number VARCHAR,
  accessible_toilet_description VARCHAR,
  accessible_toilet_description_cy VARCHAR,
  accessible_entrance BOOLEAN,
  accessible_entrance_phone_number VARCHAR,
  hearing_enhancement_equipment VARCHAR,
  lift BOOLEAN,
  lift_door_width INT,
  lift_door_limit INT,
  quiet_room BOOLEAN
);

-- COURT FACILITIES
CREATE TABLE court_facilities (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  parking BOOLEAN,
  free_water_dispensers BOOLEAN,
  snack_vending_machines BOOLEAN,
  drink_vending_machines BOOLEAN,
  cafeteria BOOLEAN,
  waiting_area BOOLEAN,
  waiting_area_children BOOLEAN,
  quiet_room BOOLEAN,
  baby_changing BOOLEAN,
  wifi BOOLEAN
);

-- COURT TRANSLATION
CREATE TABLE court_translation (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  email VARCHAR,
  phone_number VARCHAR
);

-- COURT POSTCODES
CREATE TABLE court_postcodes (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  postcode VARCHAR(10)
);

-- COURT TYPES
CREATE TABLE court_types (
  id UUID PRIMARY KEY NOT NULL,
  name VARCHAR NOT NULL
);

-- COURT CODES
CREATE TABLE court_codes (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  magistrate_court_code INTEGER,
  family_court_code INTEGER,
  tribunal_code INTEGER,
  county_court_code INTEGER,
  crown_court_code INTEGER,
  gbs INTEGER
);

-- AREA OF LAW TYPES
CREATE TABLE area_of_law_types (
  id UUID PRIMARY KEY NOT NULL,
  name VARCHAR NOT NULL,
  name_cy VARCHAR NOT NULL
);

-- COURT AREAS OF LAW
CREATE TABLE court_areas_of_law (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  areas_of_law UUID[]
);

-- COURT SINGLE POINTS OF ENTRY
CREATE TABLE court_single_points_of_entry (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  areas_of_law UUID[]
);

-- COURT CONTACT DETAILS
CREATE TABLE court_contact_details (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  court_contact_description_id UUID,
  explanation VARCHAR,
  explanation_cy VARCHAR,
  email VARCHAR,
  phone_number VARCHAR
);

-- COURT PROFESSIONAL INFORMATION
CREATE TABLE court_professional_information (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  interview_rooms BOOLEAN,
  interview_room_count INT,
  interview_phone_number VARCHAR,
  video_hearings BOOLEAN,
  common_platform BOOLEAN,
  access_scheme BOOLEAN
);

-- COURT DX CODES
CREATE TABLE court_dxcodes (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  dx_code INTEGER,
  explanation VARCHAR
);

-- COURT FAX
CREATE TABLE court_fax (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  fax_number VARCHAR,
  description VARCHAR
);

-- COURT
CREATE TABLE court (
  id UUID PRIMARY KEY NOT NULL,
  name VARCHAR NOT NULL,
  slug VARCHAR,
  open BOOLEAN,
  temporary_urgent_notice VARCHAR,
  created_at TIMESTAMP NOT NULL,
  last_updated_at TIMESTAMP NOT NULL,
  region_id UUID,
  is_service_centre BOOLEAN,
  open_on_cath BOOLEAN,
  mrd_id VARCHAR
);

-- REGION
CREATE TABLE region (
  id UUID PRIMARY KEY NOT NULL,
  name VARCHAR NOT NULL,
  county VARCHAR
);

-- COURT PHOTO
CREATE TABLE court_photo (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  file_link VARCHAR,
  last_updated_at TIMESTAMP NOT NULL,
  updated_by_user_id UUID
);

-- COURT ADDRESS
CREATE TABLE court_address (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  address_line_1 VARCHAR,
  address_line_2 VARCHAR,
  town_city VARCHAR,
  county VARCHAR,
  postcode VARCHAR,
  epim_id VARCHAR,
  lat DECIMAL(9,6),
  lon DECIMAL(9,6),
  address_type VARCHAR,
  areas_of_law UUID[],
  court_types UUID[]
);

-- OPENING HOUR TYPES
CREATE TABLE opening_hour_types (
  id UUID PRIMARY KEY NOT NULL,
  name VARCHAR,
  name_cy VARCHAR
);

-- COURT OPENING TIMES
CREATE TABLE court_opening_times (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  opening_hour_type UUID,
  day_of_week INT,
  opening_hour TIME,
  closing_hour TIME
);

-- COURT COUNTER SERVICE OPENING HOURS
CREATE TABLE court_counter_service_opening_hours (
  id UUID PRIMARY KEY NOT NULL,
  court_id UUID NOT NULL,
  counter_service BOOLEAN,
  assist_with_forms BOOLEAN,
  assist_with_documents BOOLEAN,
  assist_with_support BOOLEAN,
  appointment_needed BOOLEAN,
  appointment_contact VARCHAR,
  day_of_week INT,
  opening_hour TIME,
  closing_hour TIME
);
