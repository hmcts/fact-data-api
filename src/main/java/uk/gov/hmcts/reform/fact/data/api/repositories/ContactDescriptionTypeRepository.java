package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactDescriptionTypeRepository extends JpaRepository<ContactDescriptionType, UUID> {
}
