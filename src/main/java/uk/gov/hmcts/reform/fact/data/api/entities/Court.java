package uk.gov.hmcts.reform.fact.data.api.entities;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "court")
public class Court {

    @Schema(
        description = "The internal ID of the Court - assigned by the server during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique = true, nullable = false, insertable = false, updatable = false)
    private UUID id;

    @Schema(description = "The name of the Court")
    @NotBlank(message = "Court name must be specified")
    @Column(nullable = false)
    private String name;

    @Schema(description = "The Court 'slug'")
    @Size(min = 5, max = 200, message = "Court slug should be between 5 and 200 chars")
    @Pattern(regexp = "^[a-z-]+$", message = "Court slug must consist of only lowercase letters and hyphens")
    private String slug;

    @Schema(description = "The open status of the Court")
    private Boolean open;

    @Schema(description = "Any temporary/urgent notices attached to the Court")
    private String temporaryUrgentNotice;

    @NotNull
    @Schema(description = "The created date/time of the Court record")
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    private ZonedDateTime createdAt;

    @NotNull
    @Schema(description = "The last updated date/time of the Court record")
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    private ZonedDateTime lastUpdatedAt;

    @Schema(description = "The associated Region", accessMode = Schema.AccessMode.READ_ONLY)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "region_id")
    private Region region;

    @Schema(description = "Indicates that this Court is Service Centre")
    private Boolean isServiceCentre;

    @Schema(description = "Indicates that this Court is declared open in the Court and Tribunal Hearing service")
    private Boolean openOnCath;

    @Schema(description = "The Court's Master Reference Data ID")
    private String mrdId;

    // DTO+DAO specific handling

    @Schema(description = "The ID of the associated Region", requiredMode = Schema.RequiredMode.REQUIRED)
    @Transient // column only used by DTO
    private UUID regionId;

    @PostLoad
    public void postLoad() {
        this.regionId = Optional.ofNullable(this.getRegion()).map(Region::getId).orElse(null);
    }
}
