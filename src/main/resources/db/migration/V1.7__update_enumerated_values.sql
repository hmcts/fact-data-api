-- update service_area's type column value to match internal enum name for ServiceAreaType
update service_area set type = 'FAMILY' where type = 'family';
update service_area set type = 'CIVIL' where type = 'civil';
update service_area set type = 'OTHER' where type = 'other';

-- update service_area's catchment_method column value to match internal enum name for CatchmentMethod
update service_area set catchment_method = 'LOCAL_AUTHORITY' where catchment_method = 'local-authority';
update service_area set catchment_method = 'POSTCODE' where catchment_method = 'postcode';
update service_area set catchment_method = 'PROXIMITY' where catchment_method = 'proximity';
