INSERT INTO region (id, name, country)
SELECT gen_random_uuid(), 'Service Centre', 'England'
WHERE NOT EXISTS (
    SELECT 1 FROM region WHERE name = 'Service Centre' AND country = 'England'
);
