package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OpeningHourTypeRepository extends JpaRepository<OpeningHourType, UUID> {
}
