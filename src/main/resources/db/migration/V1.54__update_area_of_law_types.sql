-- Rename the Area of Law types for Welsh.
UPDATE area_of_law_types aolt
SET display_name_cy = v.display_name_cy
  FROM (
    VALUES
        ('Court Of Appeal', 'Y Llys Apêl'),
        ('High Court', 'Yr Uchel Lys')
) AS v(name, display_name_cy)
WHERE aolt.name = v.name;
