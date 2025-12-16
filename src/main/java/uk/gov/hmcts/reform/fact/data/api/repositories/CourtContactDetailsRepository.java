package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtContactDetails;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourtContactDetailsRepository extends JpaRepository<CourtContactDetails, UUID> {

    List<CourtContactDetails> findByCourtId(UUID courtId);

    Optional<CourtContactDetails> findByIdAndCourtId(UUID contactId, UUID courtId);

    void deleteByIdAndCourtId(UUID contactId, UUID courtId);

    boolean existsByIdAndCourtId(UUID contactId, UUID courtId);
}
