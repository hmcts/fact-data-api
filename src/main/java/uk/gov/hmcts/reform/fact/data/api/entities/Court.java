package uk.gov.hmcts.reform.fact.data.api.entities;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@Entity
@Table(name = "court")
public class Court {

    @Schema(
        description = "The internal ID - assigned by the server during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Schema(description = "The name of the Court")
    @NotBlank(message = "Court name must be specified")
    private String name;

    @Schema(description = "The Court 'slug'")
    @Size(min = 5, max = 200, message = "Court slug should be between 5 and 200 chars")
    // ideally we'd use a regex that requires a single alpha character at the start and end,
    // but we need to be careful about backtracking attacks (sonar will flag)
    @Pattern(regexp = "^[a-z-]+$", message = "Slug must match the regex '^[a-z-]+$'")
    private String slug;

    @Schema(description = "The open status of the Court")
    private Boolean open;

    @Schema(description = "Any temporary/urgent notices attached to the Court")
    private String temporaryUrgentNotice;

    @Schema(description = "The created date/time of the Court record", accessMode = Schema.AccessMode.READ_ONLY)
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    @CreationTimestamp
    @Setter(AccessLevel.NONE)
    private ZonedDateTime createdAt;

    @Schema(description = "The last updated date/time of the Court record", accessMode = Schema.AccessMode.READ_ONLY)
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    @UpdateTimestamp
    @Setter(AccessLevel.NONE)
    private ZonedDateTime lastUpdatedAt;

    @Schema(description = "The ID of the associated Region")
    @NotNull
    @Column(name = "region_id")
    private UUID regionId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", insertable = false, updatable = false)
    private Region region;

    @Schema(description = "Indicates that this Court is Service Centre")
    private Boolean isServiceCentre;

    @Schema(description = "Indicates that this Court is declared open in the Court and Tribunal Hearing service")
    private Boolean openOnCath;

    @Schema(description = "The Court's Master Reference Data ID")
    private String mrdId;

}
