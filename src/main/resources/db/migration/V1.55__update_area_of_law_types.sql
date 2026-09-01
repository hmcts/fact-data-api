-- Update area_of_law_types display names.
-- Only Welsh display_name_cy changed.
UPDATE area_of_law_types
SET display_name_cy = NULL
WHERE name IN (
               'Tax',
               'FGM',
               'Single justice procedure',
               'Divorce',
               'Court Of Appeal',
               'High Court',
               'Money claims',
               'Probate',
               'Bankruptcy',
               'Civil partnership',
               'Business and Property',
               'Adoption',
               'Forced marriage',
               'Crime',
               'Immigration'
  );

-- Both English and Welsh display names changed.
UPDATE area_of_law_types
SET display_name = NULL,
    display_name_cy = NULL
WHERE name IN (
               'Domestic abuse',
               'Financial Remedy',
               'Pathfinder – for Private Law Proceedings',
               'Civil',
               'Domestic Abuse Protection Order (DAPOs)'
  );
