-- Cater for the scenario where the data is inserted
-- but the area_of_law_id is missing from the service_area table.
-- This saves a tiny bit of performance compared to running each separately per above insert
WITH map(sa_name, aol_name) AS (VALUES ('Money claims', 'Money claims'),
  ('Tax', 'Tax'),
  ('Benefits', 'Social security'),
  ('Civil partnership', 'Civil partnership'),
  ('Probate', 'Probate'),
  ('Single Justice Procedure', 'Single justice procedure'),
  ('Childcare arrangements if you separate from your partner', 'Children'),
  ('Claims against employers', 'Employment'),
  ('High Court district registry', 'High Court District Registry'),
  ('Female Genital Mutilation Protection Orders', 'FGM'),
  ('Divorce', 'Divorce'),
  ('Bankruptcy', 'Bankruptcy'),
  ('Housing', 'Housing possession'),
  ('Domestic abuse', 'Domestic violence'),
  ('Other criminal offences', 'Crime'),
  ('Forced marriage', 'Forced marriage'),
  ('Immigration and asylum', 'Immigration'),
  ('Adoption', 'Adoption'),
  ('Financial remedy', 'Financial Remedy'))
UPDATE service_area sa
SET area_of_law_id = aol.id FROM map m
JOIN area_of_law_types aol
ON lower (aol.name) = lower (m.aol_name)
WHERE lower (sa.name) = lower (m.sa_name);

-- For the service there can be many service_areas. We need to populate
-- the array accordingly to provide the mapping
WITH mapping(service_name, area_name) AS (VALUES ('Childcare and parenting', 'Adoption'),
  ('Childcare and parenting',
  'Childcare arrangements if you separate from your partner'),

  ('Crime', 'Other criminal offences'),
  ('Crime', 'Single Justice Procedure'),

  ('Harm and abuse', 'Domestic abuse'),
  ('Harm and abuse', 'Female Genital Mutilation Protection Orders'),
  ('Harm and abuse', 'Forced marriage'),

  ('High Court district registries', 'High Court district registry'),

  ('Immigration and asylum', 'Immigration and asylum'),

  ('Money', 'Bankruptcy'),
  ('Money', 'Benefits'),
  ('Money', 'Claims against employers'),
  ('Money', 'Housing'),
  ('Money', 'Money claims'),
  ('Money', 'Probate'),
  ('Money', 'Single Justice Procedure'),
  ('Money', 'Tax'),

  ('Probate, divorce or ending civil partnerships', 'Civil partnership'),
  ('Probate, divorce or ending civil partnerships', 'Divorce'),
  ('Probate, divorce or ending civil partnerships', 'Financial remedy'),
  ('Probate, divorce or ending civil partnerships', 'Forced marriage'),
  ('Probate, divorce or ending civil partnerships', 'Probate')),
  dedup AS (
   -- one row per (service_name, area id/name)
   SELECT DISTINCT m.service_name,
                   sa.id   AS sa_id,
                   sa.name AS sa_name
   FROM mapping m
          JOIN service_area sa
               ON lower(sa.name) = lower(m.area_name)),
  agg AS (SELECT service_name,
                array_agg(sa_id ORDER BY sa_name) AS ids
         FROM dedup
         GROUP BY service_name)
UPDATE service s
SET service_areas = COALESCE(agg.ids, ARRAY[]::uuid[]) FROM agg
WHERE lower (s.name) = lower (agg.service_name);
