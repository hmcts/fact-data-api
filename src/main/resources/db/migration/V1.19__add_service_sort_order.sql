ALTER TABLE public.service ADD COLUMN sort_order integer;

UPDATE public.service SET sort_order = 1 WHERE name = 'Money';
UPDATE public.service SET sort_order = 2 WHERE name = 'Probate, divorce or ending civil partnerships';
UPDATE public.service SET sort_order = 3 WHERE name = 'Childcare and parenting';
UPDATE public.service SET sort_order = 4 WHERE name = 'Harm and abuse';
UPDATE public.service SET sort_order = 5 WHERE name = 'Immigration and asylum';
UPDATE public.service SET sort_order = 6 WHERE name = 'Crime';
UPDATE public.service SET sort_order = 7 WHERE name = 'High Court district registries';
