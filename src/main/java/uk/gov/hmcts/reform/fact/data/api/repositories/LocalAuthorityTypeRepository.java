package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocalAuthorityTypeRepository extends JpaRepository<LocalAuthorityType, UUID> {
    Optional<LocalAuthorityType> findByName(String name);
}
