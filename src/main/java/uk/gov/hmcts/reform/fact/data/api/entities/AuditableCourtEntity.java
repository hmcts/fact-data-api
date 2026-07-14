package uk.gov.hmcts.reform.fact.data.api.entities;

import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;

import java.util.UUID;

public interface AuditableCourtEntity extends AuditableEntity {
    UUID getCourtId();

    @Override
    default UUID getAuditSubjectId() {
        return getCourtId();
    }

    @Override
    default SubjectType getAuditSubjectType() {
        return SubjectType.COURT;
    }
}
