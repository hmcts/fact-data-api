package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceAreas;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtServiceAreasRepository extends JpaRepository<CourtServiceAreas, UUID> {
    /**
     * Finds court service area links for a service area id.
     *
     * @param id the service area id
     * @return matching court service areas
     */
    // Note that @Type(ListArrayType.class) requires for this instance we use a native query
    @Query(
        value = "SELECT * FROM court_service_areas c WHERE :id = ANY(c.service_area_id)",
        nativeQuery = true
    )
    List<CourtServiceAreas> findByServiceAreaId(UUID id);

    /**
     * Finds nearest open courts for a service area by distance.
     *
     * @param serviceAreaId the service area id
     * @param lat the latitude to search from
     * @param lon the longitude to search from
     * @param limit the maximum number of results
     * @return matching courts with distance data
     */
    @Query(
        value = """
            SELECT
                c.id   AS courtId,
                c.name AS courtName,
                c.slug AS courtSlug,
                (
                  point(CAST(ca.lon AS float8), CAST(ca.lat AS float8))
                  <@>
                  point(CAST(:lon AS float8), CAST(:lat AS float8))
                ) AS distance
            FROM court_service_areas csa
            JOIN court c
              ON c.id = csa.court_id
            JOIN court_address ca
              ON ca.court_id = c.id
            WHERE CAST(:serviceAreaId AS uuid) = ANY(csa.service_area_id)
              AND c.open = true
              AND ca.address_type IN ('VISIT_US', 'VISIT_OR_CONTACT_US')
              AND ca.lat IS NOT NULL
              AND ca.lon IS NOT NULL
            ORDER BY distance, c.name
            LIMIT :limit
            """,
        nativeQuery = true
    )
    List<CourtWithDistance> findNearestByServiceAreaDistance(
        @Param("serviceAreaId") UUID serviceAreaId,
        @Param("lat") double lat,
        @Param("lon") double lon,
        @Param("limit") int limit
    );
}
