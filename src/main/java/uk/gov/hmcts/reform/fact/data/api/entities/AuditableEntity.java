package uk.gov.hmcts.reform.fact.data.api.entities;

import uk.gov.hmcts.reform.fact.data.api.entities.types.AuditSubjectType;

import java.util.UUID;

public interface AuditableEntity {
    UUID getId();

    UUID getAuditSubjectId();

    AuditSubjectType getAuditSubjectType();
}
