-- FACT-3222 clear display_name / display_name_cy so the frontend falls back to
-- name / name_cy ("Social security" / "Nawdd Cymdeithasol")

UPDATE area_of_law_types
SET display_name = NULL,
    display_name_cy = NULL
WHERE name = 'Social security';

