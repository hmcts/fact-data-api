-- Update Welsh translations for opening hour types
UPDATE public.opening_hour_types SET name_cy = 'Oriau agor Llys y Goron' WHERE name = 'Crown Court open';
UPDATE public.opening_hour_types SET name_cy = 'Oriau agor y Tribiwnlys' WHERE name = 'Tribunal open';
