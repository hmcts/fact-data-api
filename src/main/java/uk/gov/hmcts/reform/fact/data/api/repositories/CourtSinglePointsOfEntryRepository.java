package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtSinglePointsOfEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtSinglePointsOfEntryRepository extends JpaRepository<CourtSinglePointsOfEntry, UUID> {

    /**
     * Finds the nearest SPOE court for the Children area of law.
     *
     * @param lat the latitude to search from
     * @param lon the longitude to search from
     * @return the nearest court with distance data
     */
    @Query(
        value = """
            WITH children_aol AS (
              SELECT id
              FROM area_of_law_types
              WHERE name = 'Children'
              LIMIT 1
            ),
            spoe_courts AS (
              SELECT DISTINCT ON (c.id)
                c.id   AS courtId,
                c.name AS courtName,
                c.slug AS courtSlug,
                (
                  point(CAST(ca.lon AS float8), CAST(ca.lat AS float8))
                  <@>
                  point(CAST(:lon AS float8), CAST(:lat AS float8))
                ) AS distance
              FROM court c
              JOIN court_single_points_of_entry spoe
                ON spoe.court_id = c.id
              JOIN children_aol aol
                ON aol.id = ANY(spoe.areas_of_law)
              JOIN court_address ca
                ON ca.court_id = c.id
              WHERE c.open = true
                AND ca.address_type IN ('VISIT_US', 'VISIT_OR_CONTACT_US')
                AND ca.lat IS NOT NULL
                AND ca.lon IS NOT NULL
              ORDER BY c.id, distance
            )
            SELECT courtId, courtName, courtSlug, distance
            FROM spoe_courts
            ORDER BY distance
            LIMIT 1
            """,
        nativeQuery = true
    )
    List<CourtWithDistance> findNearestCourtBySpoeAndChildrenAreaOfLaw(
        @Param("lat") double lat,
        @Param("lon") double lon
    );
    Optional<CourtSinglePointsOfEntry> findByCourtId(UUID courtId);
}
