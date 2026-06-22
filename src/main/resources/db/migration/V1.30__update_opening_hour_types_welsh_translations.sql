-- Update Welsh translations for opening hour types
UPDATE public.opening_hour_types SET name_cy = 'Derbynnir taliadau dros y ffôn' WHERE name = 'Telephone payments accepted';
UPDATE public.opening_hour_types SET name_cy = 'Ateb ymholiadau dros y ffôn' WHERE name = 'Telephone enquiries answered';
UPDATE public.opening_hour_types SET name_cy = 'Llys y Goron ar agor' WHERE name = 'Crown Court open';
UPDATE public.opening_hour_types SET name_cy = 'Tribiwnlys ar agor' WHERE name = 'Tribunal open';
UPDATE public.opening_hour_types SET name_cy = 'Llys ar agor' WHERE name = 'Court open';
