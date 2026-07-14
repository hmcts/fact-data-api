package uk.gov.hmcts.reform.fact.data.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;

import java.util.UUID;

public interface AuditableEntity {
    UUID getId();

    @JsonIgnore
    UUID getAuditSubjectId();

    @JsonIgnore
    SubjectType getAuditSubjectType();
}
