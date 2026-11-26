-- FACT-3338: Rename appointment_contact to appointment_email
-- The column holds the email address for booking an appointment, a separate
-- appointment_phone column now exists, so "contact" is ambiguous.
ALTER TABLE court_counter_service_opening_hours
  RENAME COLUMN appointment_contact TO appointment_email;
