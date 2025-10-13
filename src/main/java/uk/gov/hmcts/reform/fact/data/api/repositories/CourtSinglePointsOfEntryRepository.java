package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtSinglePointsOfEntry;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtSinglePointsOfEntryRepository extends JpaRepository<CourtSinglePointsOfEntry, UUID> {
}
