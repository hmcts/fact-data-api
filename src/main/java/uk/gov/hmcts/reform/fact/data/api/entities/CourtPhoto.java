package uk.gov.hmcts.reform.fact.data.api.entities;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "court_photo")
public class CourtPhoto extends BaseCourtEntity {

    @Schema(description = "Link to the image file")
    @NotBlank(message = "Link to the image file must be specified")
    private String fileLink;

    @Schema(description = "The last update date/time", accessMode = Schema.AccessMode.READ_ONLY)
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    @UpdateTimestamp
    @Setter(AccessLevel.NONE)
    private ZonedDateTime lastUpdatedAt;

    @Schema(description = "The ID of the associated Court")
    @NotNull
    @Column(name = "updated_by_user_id")
    private UUID updatedByUserId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id", insertable = false, updatable = false)
    private User updatedByUser;

}
