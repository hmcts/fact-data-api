package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;

@Repository
public interface CourtAddressRepository extends JpaRepository<CourtAddress, UUID> {

    @Query(value = """
        SELECT
            c.id as courtId,
            c.name as courtName,
            c.slug as courtSlug,
            (point(ca.lon, ca.lat) <@> point(:lon, :lat)) as distance
        FROM court_address ca
        JOIN court c ON c.id = ca.court_id
        WHERE ca.address_type IN ('VISIT_US', 'VISIT_OR_CONTACT_US')
          AND ca.lat IS NOT NULL
          AND ca.lon IS NOT NULL
          AND c.open = true
        ORDER BY distance
        LIMIT :limit
    """, nativeQuery = true)
    List<CourtWithDistance> findNearestCourts(
        @Param("lat") double lat,
        @Param("lon") double lon,
        @Param("limit") int limit
    );
}
