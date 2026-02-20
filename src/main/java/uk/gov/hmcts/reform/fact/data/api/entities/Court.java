package uk.gov.hmcts.reform.fact.data.api.entities;

import uk.gov.hmcts.reform.fact.data.api.audit.AuditableCourtEntityListener;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Entity
@EntityListeners(AuditableCourtEntityListener.class)
@Table(name = "court")
public class Court extends AbstractCourtEntity implements AuditableCourtEntity {
    /**
     * This is required by the {@link AuditableCourtEntity} superclass. It's provided automatically for all entities
     * that have a specific courtId field, with this case being the outlier where the id field is actually the courtId.
     *
     * @return the court id.
     */
    @Override
    @JsonIgnore
    public UUID getCourtId() {
        return getId();
    }
}
