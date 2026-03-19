package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.springframework.data.jpa.repository.Query;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceAreas;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtServiceAreasRepository extends JpaRepository<CourtServiceAreas, UUID> {
    /**
     * Finds court service area links for a service area id.
     * Note that this query still uses native SQL to match against the Postgres array column with ANY(...).
     *
     * @param id the service area id
     * @return matching court service areas
     */
    @Query(
        value = "SELECT * FROM court_service_areas c WHERE :id = ANY(c.service_area_id)",
        nativeQuery = true
    )
    List<CourtServiceAreas> findByServiceAreaId(UUID id);

    /**
     * checks that a court service area link exists for a set of service area id and catchment types.
     *
     * @param id the service area id
     * @param catchmentTypes the list of catchment types
     * @return true if a matching court service area exists, false otherwise
     */
    @Query(
        value = """
            SELECT EXISTS (
                SELECT
                    csa.id
                FROM
                    court_service_areas csa
                WHERE
                    :id = ANY(csa.service_area_id)
                AND
                    csa.catchment_type IN (:#{#catchmentTypes.![name()]})
                LIMIT 1
            )
            """,
        nativeQuery = true
    )
    boolean existsByServiceAreaIdAndCatchmentTypeIn(UUID id, List<CatchmentType> catchmentTypes);
}
