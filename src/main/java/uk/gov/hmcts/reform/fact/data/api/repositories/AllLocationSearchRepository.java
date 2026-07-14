package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.fact.data.api.dto.AllLocationSearchResult;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;

import java.util.List;
import java.util.UUID;

/**
 * Searches courts and service centres with a single relevance ranking.
 */
public interface AllLocationSearchRepository extends Repository<Court, UUID> {

    /**
     * Searches open locations by name or public address fields.
     *
     * @param query trimmed query string
     * @return location identifiers in global relevance order
     */
    @Query(
        value = """
            WITH ranked_locations AS (
                SELECT
                    c.id,
                    c.name,
                    'COURT' AS location_type,
                    MAX(CASE WHEN REPLACE(COALESCE(ca.postcode, ''), ' ', '')
                                      ILIKE REPLACE(CONCAT('%', :query, '%'), ' ', '')
                             THEN 1 ELSE 0 END) AS rank_postcode,
                    MAX(CASE WHEN c.name ILIKE CONCAT('%', :query, '%')
                             THEN 1 ELSE 0 END) AS rank_name,
                    MAX(CASE WHEN COALESCE(ca.town_city, '') ILIKE CONCAT('%', :query, '%')
                             THEN 1 ELSE 0 END) AS rank_town,
                    MAX(CASE WHEN COALESCE(ca.address_line_1, '') ILIKE CONCAT('%', :query, '%')
                             THEN 1 ELSE 0 END) AS rank_addr1,
                    MAX(CASE WHEN COALESCE(ca.address_line_2, '') ILIKE CONCAT('%', :query, '%')
                             THEN 1 ELSE 0 END) AS rank_addr2
                FROM court c
                LEFT JOIN court_address ca
                  ON ca.court_id = c.id
                 AND ca.address_type IN ('VISIT_US', 'VISIT_OR_CONTACT_US')
                WHERE c.open = TRUE
                  AND (
                       c.name ILIKE CONCAT('%', :query, '%')
                    OR ca.address_line_1 ILIKE CONCAT('%', :query, '%')
                    OR ca.address_line_2 ILIKE CONCAT('%', :query, '%')
                    OR ca.town_city ILIKE CONCAT('%', :query, '%')
                    OR ca.county ILIKE CONCAT('%', :query, '%')
                    OR REPLACE(COALESCE(ca.postcode, ''), ' ', '')
                         ILIKE REPLACE(CONCAT('%', :query, '%'), ' ', '')
                  )
                GROUP BY c.id, c.name

                UNION ALL

                SELECT
                    sc.id,
                    sc.name,
                    'SERVICE_CENTRE' AS location_type,
                    MAX(CASE WHEN REPLACE(COALESCE(sca.postcode, ''), ' ', '')
                                      ILIKE REPLACE(CONCAT('%', :query, '%'), ' ', '')
                             THEN 1 ELSE 0 END) AS rank_postcode,
                    MAX(CASE WHEN sc.name ILIKE CONCAT('%', :query, '%')
                             THEN 1 ELSE 0 END) AS rank_name,
                    MAX(CASE WHEN COALESCE(sca.town_city, '') ILIKE CONCAT('%', :query, '%')
                             THEN 1 ELSE 0 END) AS rank_town,
                    MAX(CASE WHEN COALESCE(sca.address_line_1, '') ILIKE CONCAT('%', :query, '%')
                             THEN 1 ELSE 0 END) AS rank_addr1,
                    MAX(CASE WHEN COALESCE(sca.address_line_2, '') ILIKE CONCAT('%', :query, '%')
                             THEN 1 ELSE 0 END) AS rank_addr2
                FROM service_centre sc
                LEFT JOIN service_centre_address sca
                  ON sca.service_centre_id = sc.id
                 AND sca.address_type IN ('VISIT_US', 'VISIT_OR_CONTACT_US')
                WHERE sc.open = TRUE
                  AND (
                       sc.name ILIKE CONCAT('%', :query, '%')
                    OR sca.address_line_1 ILIKE CONCAT('%', :query, '%')
                    OR sca.address_line_2 ILIKE CONCAT('%', :query, '%')
                    OR sca.town_city ILIKE CONCAT('%', :query, '%')
                    OR sca.county ILIKE CONCAT('%', :query, '%')
                    OR REPLACE(COALESCE(sca.postcode, ''), ' ', '')
                         ILIKE REPLACE(CONCAT('%', :query, '%'), ' ', '')
                  )
                GROUP BY sc.id, sc.name
            )
            SELECT id, location_type AS "locationType"
            FROM ranked_locations
            ORDER BY
                rank_postcode DESC,
                rank_name DESC,
                rank_town DESC,
                rank_addr1 DESC,
                rank_addr2 DESC,
                name ASC,
                location_type ASC,
                id ASC
            """,
        nativeQuery = true
    )
    List<AllLocationSearchResult> searchOpenByNameOrAddress(@Param("query") String query);
}
