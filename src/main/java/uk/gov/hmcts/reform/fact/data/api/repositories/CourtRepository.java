package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtRepository extends JpaRepository<Court, UUID> {
    /**
     * Finds courts by region ids and partial name, case-insensitive.
     *
     * @param regionIds the region identifiers to filter by
     * @param name the name fragment to match
     * @param pageable the pagination configuration
     * @return the matching courts
     */
    Page<Court> findByRegionIdInAndNameContainingIgnoreCase(
        List<UUID> regionIds,
        String name,
        Pageable pageable
    );

    /**
     * Finds open courts by region ids and partial name, case-insensitive.
     *
     * @param regionIds the region identifiers to filter by
     * @param name the name fragment to match
     * @param pageable the pagination configuration
     * @return the matching courts
     */
    Page<Court> findByRegionIdInAndOpenTrueAndNameContainingIgnoreCase(
        List<UUID> regionIds,
        String name,
        Pageable pageable
    );

    /**
     * Checks whether a court slug already exists.
     *
     * @param slug the slug to check
     * @return true if the slug exists
     */
    boolean existsBySlug(String slug);

    /**
     * Finds courts whose names start with the provided prefix.
     *
     * @param namePrefix the name prefix
     * @return matching courts
     */
    List<Court> findByNameStartingWithIgnoreCase(String namePrefix);

    /**
     * Finds courts by name prefix and open flag ordered by name.
     *
     * @param prefix the name prefix
     * @param active the open flag
     * @return matching courts
     */
    List<Court> findCourtByNameStartingWithIgnoreCaseAndOpenOrderByNameAsc(
        String prefix,
        boolean active
    );

    /**
     * Searches open courts by name or address with ranking.
     *
     * @param query the query string
     * @return matching courts
     */
    @Query(
        value = """
            SELECT  sub.*
            FROM (
              SELECT DISTINCT
                  c.*,
                  CASE WHEN REPLACE(COALESCE(ca.postcode, ''), ' ', '')
                            ILIKE REPLACE(CONCAT('%', :query, '%'), ' ', '')
                       THEN 1 ELSE 0 END AS rank_postcode,
                  CASE WHEN c.name ILIKE CONCAT('%', :query, '%') THEN 1 ELSE 0 END AS rank_name,
                  CASE WHEN COALESCE(ca.town_city, '') ILIKE CONCAT('%', :query, '%') THEN 1 ELSE 0 END AS rank_town,
                  CASE WHEN COALESCE(ca.address_line_1, '') ILIKE CONCAT('%', :query, '%')
                       THEN 1 ELSE 0 END AS rank_addr1,
                  CASE WHEN COALESCE(ca.address_line_2, '') ILIKE CONCAT('%', :query, '%')
                       THEN 1 ELSE 0 END AS rank_addr2
              FROM court c
              LEFT JOIN court_address ca
                ON ca.court_id = c.id
               AND ca.address_type IN ('VISIT_US', 'VISIT_OR_CONTACT_US')
              WHERE c.open = TRUE
                AND (
                     c.name ILIKE CONCAT('%', :query, '%')
                  OR ca.address_line_1 ILIKE CONCAT('%', :query, '%')
                  OR ca.address_line_2 ILIKE CONCAT('%', :query, '%')
                  OR ca.town_city      ILIKE CONCAT('%', :query, '%')
                  OR ca.county         ILIKE CONCAT('%', :query, '%')
                  OR REPLACE(COALESCE(ca.postcode, ''), ' ', '')
                       ILIKE REPLACE(CONCAT('%', :query, '%'), ' ', '')
                )
            ) sub
            ORDER BY
              sub.rank_postcode DESC,
              sub.rank_name     DESC,
              sub.rank_town     DESC,
              sub.rank_addr1    DESC,
              sub.rank_addr2    DESC,
              sub.name ASC;
            """, nativeQuery = true
    )
    List<Court> searchOpenByNameOrAddress(@Param("query") String query);
}
