package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtFax;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtFaxRepository extends JpaRepository<CourtFax, UUID> {

    List<CourtFax> findAllByCourtId(UUID courtId);

    void deleteAllByCourtId(UUID courtId);
}
