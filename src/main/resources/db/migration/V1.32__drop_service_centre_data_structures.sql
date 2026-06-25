ALTER TABLE court DROP COLUMN IF EXISTS is_service_centre;

DROP TRIGGER IF EXISTS court_service_areas_court_update_last_updated_at_trg ON court_service_areas;

DROP TABLE IF EXISTS court_service_areas CASCADE;

DELETE FROM region
WHERE name = 'Service Centre'
  AND country = 'England';
