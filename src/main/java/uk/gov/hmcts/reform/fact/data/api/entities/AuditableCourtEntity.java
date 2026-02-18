package uk.gov.hmcts.reform.fact.data.api.entities;

import java.util.UUID;

public interface AuditableCourtEntity {
    UUID getId();

    UUID getCourtId();
}
