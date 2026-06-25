WITH mapping(service_name, area_name) AS (VALUES
  ('Childcare and parenting', 'Adoption'),
  ('Childcare and parenting', 'Childcare arrangements if you separate from your partner'),
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
  ('Probate, divorce or ending civil partnerships', 'Probate')
),
dedup AS (
  SELECT DISTINCT m.service_name,
                  sa.id AS service_area_id,
                  sa.sort_order,
                  sa.name
  FROM mapping m
    JOIN service_area sa
      ON lower(sa.name) = lower(m.area_name)
),
agg AS (
  SELECT service_name,
         array_agg(service_area_id ORDER BY sort_order nulls last, name) AS service_area_ids
  FROM dedup
  GROUP BY service_name
)
UPDATE service s
SET service_areas = agg.service_area_ids
FROM agg
WHERE lower(s.name) = lower(agg.service_name);
