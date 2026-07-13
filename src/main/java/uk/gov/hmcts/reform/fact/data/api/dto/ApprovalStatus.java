package uk.gov.hmcts.reform.fact.data.api.dto;

import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AuditSubjectType;

import java.time.ZonedDateTime;
import java.util.UUID;

public record ApprovalStatus(
    UUID subjectId,
    AuditSubjectType subjectType,
    String name,
    boolean approved,
    UUID approvalId,
    UUID userId,
    User user,
    ZonedDateTime lastUpdatedAt
) {
}
