-- Rename the Area of Law type from 'Domestic violence' to 'Domestic abuse' and update Welsh text as well.
UPDATE public.area_of_law_types SET name = 'Domestic abuse', name_cy = 'Cam-drin domestig' WHERE name = 'Domestic violence';

