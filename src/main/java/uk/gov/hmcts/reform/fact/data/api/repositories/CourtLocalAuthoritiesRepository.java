package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtLocalAuthorities;

import java.util.Collection;
import java.util.UUID;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtLocalAuthoritiesRepository extends JpaRepository<CourtLocalAuthorities, UUID> {
    Optional<CourtLocalAuthorities> findByCourtIdAndAreaOfLawId(UUID courtId, UUID areaOfLawId);

    boolean existsByAreaOfLawId(UUID areaOfLawId);

    void deleteByCourtId(UUID courtId);

    /**
     * Removes all local authority records for a given court that DO NOT pertain
     * to the passed in set of areas of law.
     *
     * @param courtId the court ID
     * @param areaOfLawId the collection of areas of law to maintain
     */
    void deleteByCourtIdAndAreaOfLawIdNotIn(final @NotNull UUID courtId, final Collection<UUID> areaOfLawId);
}
