-- FACT-3338: optional phone number for booking counter service appointments
ALTER TABLE court_counter_service_opening_hours ADD COLUMN appointment_phone VARCHAR(20);
