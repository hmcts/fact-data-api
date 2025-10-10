package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtFax;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtFaxRepository extends JpaRepository<CourtFax, UUID> {
}
