package uk.gov.hmcts.reform.fact.data.api.dto;

import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;

import java.time.ZonedDateTime;
import java.util.UUID;

public record ApprovalStatus(
    UUID subjectId,
    SubjectType subjectType,
    String name,
    boolean approved,
    UUID approvalId,
    UUID userId,
    User user,
    ZonedDateTime lastUpdatedAt
) {
}
