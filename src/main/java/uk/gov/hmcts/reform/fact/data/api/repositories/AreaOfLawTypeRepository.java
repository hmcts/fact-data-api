package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AreaOfLawTypeRepository extends JpaRepository<AreaOfLawType, UUID> {
    Optional<AreaOfLawType> findByName(String name);

    Optional<AreaOfLawType> findByNameIgnoreCase(String name);

    List<AreaOfLawType> findByNameIn(List<String> names);
}
