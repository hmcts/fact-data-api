-- Normalise all existing address text once, using the same rules as OsAddressFormatter.
CREATE FUNCTION pg_temp.fact_title_case_address(value TEXT) RETURNS TEXT
LANGUAGE plpgsql IMMUTABLE STRICT AS $$
DECLARE
    formatted TEXT := '';
    current_character TEXT;
    capitalise_next BOOLEAN := TRUE;
    character_index INTEGER;
BEGIN
    FOR character_index IN 1..char_length(value) LOOP
        current_character := lower(substr(value, character_index, 1));
        IF current_character ~ '[[:alpha:]]' THEN
            formatted := formatted || CASE
                WHEN capitalise_next THEN upper(current_character) ELSE current_character END;
            capitalise_next := FALSE;
        ELSE
            formatted := formatted || current_character;
            IF current_character !~ '[[:digit:]]' AND current_character NOT IN ('''', chr(8217)) THEN
                capitalise_next := TRUE;
            END IF;
        END IF;
    END LOOP;
    RETURN formatted;
END;
$$;

UPDATE court_address
SET address_line_1 = pg_temp.fact_title_case_address(address_line_1),
    address_line_2 = pg_temp.fact_title_case_address(address_line_2),
    town_city = pg_temp.fact_title_case_address(town_city),
    county = pg_temp.fact_title_case_address(county);

UPDATE service_centre_address
SET address_line_1 = pg_temp.fact_title_case_address(address_line_1),
    address_line_2 = pg_temp.fact_title_case_address(address_line_2),
    town_city = pg_temp.fact_title_case_address(town_city),
    county = pg_temp.fact_title_case_address(county);

DROP FUNCTION pg_temp.fact_title_case_address(TEXT);
