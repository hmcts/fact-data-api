package uk.gov.hmcts.reform.fact.data.api.entities;

import uk.gov.hmcts.reform.fact.data.api.entities.types.AuditSubjectType;

import java.util.UUID;

public interface AuditableCourtEntity extends AuditableEntity {
    UUID getCourtId();

    @Override
    default UUID getAuditSubjectId() {
        return getCourtId();
    }

    @Override
    default AuditSubjectType getAuditSubjectType() {
        return AuditSubjectType.COURT;
    }
}
