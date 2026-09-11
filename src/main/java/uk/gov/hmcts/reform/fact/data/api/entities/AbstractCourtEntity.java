package uk.gov.hmcts.reform.fact.data.api.entities;

import uk.gov.hmcts.reform.fact.data.api.controllers.CourtController.CourtDetailsView;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidCourtSlug;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonView(CourtDetailsView.class)
public abstract class AbstractCourtEntity {

    @Schema(
        description = "The internal ID - assigned by the server during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Schema(description = "The name of the Court")
    @NotBlank(message = "Court name must be specified")
    @Size(min = ValidationConstants.COURT_NAME_MIN_LENGTH, max = ValidationConstants.COURT_NAME_MAX_LENGTH,
        message = ValidationConstants.COURT_NAME_LENGTH_MESSAGE)
    @Pattern(
        regexp = ValidationConstants.COURT_NAME_REGEX,
        message = ValidationConstants.COURT_NAME_REGEX_MESSAGE
    )
    private String name;

    @Schema(description = "The Court 'slug'")
    @ValidCourtSlug
    @Column(unique = true)
    private String slug;

    @Schema(description = "The open status of the Court")
    @NotNull(message = "Court open status must be specified")
    private Boolean open;

    @Schema(description = "Any warning notices attached to the Court")
    @Size(max = ValidationConstants.COMMON_TEXT_MAX_LENGTH,
        message = ValidationConstants.WARNING_NOTICE_MAX_LENGTH_MESSAGE)
    @Pattern(
        regexp = ValidationConstants.WARNING_NOTICE_REGEX,
        message = ValidationConstants.WARNING_NOTICE_REGEX_MESSAGE
    )
    private String warningNotice;

    @Schema(description = "Any Welsh warning notices attached to the Court")
    @Size(max = ValidationConstants.COMMON_TEXT_MAX_LENGTH,
        message = ValidationConstants.WELSH_WARNING_NOTICE_MAX_LENGTH_MESSAGE)
    @Pattern(
        regexp = ValidationConstants.WELSH_WARNING_NOTICE_REGEX,
        message = ValidationConstants.WELSH_WARNING_NOTICE_REGEX_MESSAGE
    )
    private String warningNoticeCy;

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
    @Column(name = "region_id", nullable = false)
    private UUID regionId;

    @Schema(description = "Indicates that this Court is declared open in the Court and Tribunal Hearing service")
    private Boolean openOnCath;

    @Schema(description = "The Court's Master Reference Data ID")
    private String mrdId;
}
