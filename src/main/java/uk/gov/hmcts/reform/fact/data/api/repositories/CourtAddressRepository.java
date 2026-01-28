package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtAddressRepository extends JpaRepository<CourtAddress, UUID> {

    List<CourtAddress> findByCourtId(UUID courtId);

    Optional<CourtAddress> findByIdAndCourtId(UUID addressId, UUID courtId);

    void deleteByIdAndCourtId(UUID addressId, UUID courtId);

    boolean existsByIdAndCourtId(UUID addressId, UUID courtId);
}
