CREATE TABLE postcode_reference (
    postcode VARCHAR(8) PRIMARY KEY,
    status VARCHAR(10) NOT NULL,
    is_large_user BOOLEAN NOT NULL,
    introduced_yyyymm CHAR(6) NOT NULL,
    terminated_yyyymm CHAR(6),
    coordinate_quality SMALLINT NOT NULL,
    has_coordinates BOOLEAN NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    CONSTRAINT postcode_reference_status_check
        CHECK (status IN ('current', 'terminated')),
    CONSTRAINT postcode_reference_coordinate_quality_check
        CHECK (coordinate_quality IN (1, 2, 3, 4, 5, 6, 8, 9)),
    CONSTRAINT postcode_reference_termination_check
        CHECK ((status = 'terminated') = (terminated_yyyymm IS NOT NULL)),
    CONSTRAINT postcode_reference_coordinates_check
        CHECK (has_coordinates = (latitude IS NOT NULL AND longitude IS NOT NULL)),
    CONSTRAINT postcode_reference_exception_scope_check
        CHECK (is_large_user OR status = 'terminated')
);
