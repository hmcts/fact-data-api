-- Update Welsh translations for opening hour types: replace 'CHANGE ME / NEED WELSH' placeholder with correct Welsh translations
UPDATE public.opening_hour_types SET name_cy = 'Dim gwasanaeth cownter ar gael' WHERE name = 'No counter service available';
