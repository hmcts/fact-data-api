package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtDxCode;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtDxCodeRepository extends JpaRepository<CourtDxCode, UUID> {

    List<CourtDxCode> findAllByCourtId(UUID courtId);

    void deleteAllByCourtId(UUID courtId);
}
