package uk.gov.hmcts.reform.fact.data.api.entities;

import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "court_lock")
public class CourtLock extends BaseCourtEntity {

    @Schema(description = "The ID of the associated User")
    @NotNull
    @Column(name = "user_id")
    private UUID userId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Schema(description = "The page being locked")
    @NotNull
    @Enumerated(EnumType.STRING)
    private Page page;

    // TODO - consider using @UpdateTimestamp here to automate lock timestamping?
    // as the intention is that a lock can be re-acquired to prevent
    // being timed out, it may be prudent to automate the value in this field
    // using an @UpdateTimestamp as well as removing the Setter.
    @Schema(description = "Acquire timestamp for the lock")
    @NotNull
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    private ZonedDateTime lockAcquired;

}
