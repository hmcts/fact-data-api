package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceAreas;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtServiceAreasRepository extends JpaRepository<CourtServiceAreas, UUID> {
    // Note that @Type(ListArrayType.class) requires for this instance we use a native query
    @Query(
        value = "SELECT * FROM court_service_areas c WHERE :id = ANY(c.service_area_id)",
        nativeQuery = true
    )
    List<CourtServiceAreas> findByServiceAreaId(UUID id);
}
