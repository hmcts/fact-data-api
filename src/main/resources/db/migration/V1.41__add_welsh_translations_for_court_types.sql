ALTER TABLE court_types
    ADD COLUMN IF NOT EXISTS name_cy VARCHAR;

UPDATE court_types ct
SET name_cy = v.name_cy
FROM (
    VALUES
        ('Magistrates'' Court', 'Llys Ynadon'),
        ('Family Court', 'Llys Teulu'),
        ('Tribunal', 'Tribiwnlys'),
        ('County Court', 'Llys Sirol'),
        ('Crown Court', 'Llys y Goron')
) AS v(name, name_cy)
WHERE ct.name = v.name;

