package uk.gov.hmcts.reform.fact.data.api.entities;


import uk.gov.hmcts.reform.fact.data.api.audit.AuditableCourtEntityListener;

import java.util.UUID;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

@EntityListeners(AuditableCourtEntityListener.class)
@MappedSuperclass
public abstract class AuditableCourtEntity {
    public abstract UUID getId();

    public abstract UUID getCourtId();
}
