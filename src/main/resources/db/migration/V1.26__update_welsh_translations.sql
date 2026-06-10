-- Update Welsh translations for contact description types: replace 'CHANGE ME / NEED WELSH' placeholder with correct Welsh translations
UPDATE public.contact_description_types SET name_cy = 'Ymholiadau Llys y Goron' WHERE name = 'Crown Court enquiries';
UPDATE public.contact_description_types SET name_cy = 'Ymholiadau' WHERE name = 'Enquiries';
UPDATE public.contact_description_types SET name_cy = 'Ymholiadau Llys Teulu' WHERE name = 'Family court enquiries';
UPDATE public.contact_description_types SET name_cy = 'Gwasanaeth rheithgor' WHERE name = 'Jury service';
UPDATE public.contact_description_types SET name_cy = 'Talu ffi' WHERE name = 'Pay a fee';
UPDATE public.contact_description_types SET name_cy = 'Trawsgrifiadau' WHERE name = 'Transcripts';

