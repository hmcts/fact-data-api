package uk.gov.hmcts.reform.fact.data.api.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.fact.data.api.audit.AuditableCourtEntityListener;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidCourtSlug;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@EntityListeners(AuditableCourtEntityListener.class)
@Table(name = "service_centre")
public class ServiceCentre implements AuditableEntity {

    @Schema(
        description = "The internal ID - assigned by the server during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Schema(description = "The name of the Service Centre")
    @NotBlank(message = "Service centre name must be specified")
    @Size(min = ValidationConstants.COURT_SERVICE_CENTRE_NAME_MIN_LENGTH,
        max = ValidationConstants.COURT_SERVICE_CENTRE_NAME_MAX_LENGTH,
        message = ValidationConstants.SERVICE_CENTRE_NAME_LENGTH_MESSAGE)
    @Pattern(
        regexp = ValidationConstants.COURT_SERVICE_CENTRE_NAME_REGEX,
        message = ValidationConstants.SERVICE_CENTRE_NAME_REGEX_MESSAGE
    )
    private String name;

    @Schema(description = "The Service Centre 'slug'")
    @ValidCourtSlug
    @Column(unique = true)
    private String slug;

    @Schema(description = "The open status of the Service Centre")
    @NotNull(message = "Service centre open status must be specified")
    private Boolean open;

    @Schema(description = "Any warning notices attached to the Service Centre")
    @Size(max = ValidationConstants.COMMON_TEXT_MAX_LENGTH,
        message = ValidationConstants.WARNING_NOTICE_MAX_LENGTH_MESSAGE)
    @Pattern(
        regexp = ValidationConstants.WARNING_NOTICE_REGEX,
        message = ValidationConstants.WARNING_NOTICE_REGEX_MESSAGE
    )
    @Column(name = "warning_notice")
    private String warningNotice;

    @Schema(description = "Any Welsh warning notices attached to the Service Centre")
    @Size(max = ValidationConstants.COMMON_TEXT_MAX_LENGTH,
        message = ValidationConstants.WELSH_WARNING_NOTICE_MAX_LENGTH_MESSAGE)
    @Pattern(
        regexp = ValidationConstants.WELSH_WARNING_NOTICE_REGEX,
        message = ValidationConstants.WELSH_WARNING_NOTICE_REGEX_MESSAGE
    )
    @Column(name = "warning_notice_cy")
    private String warningNoticeCy;

    @Schema(
        description = "The created date/time of the Service Centre record",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    @CreationTimestamp
    @Setter(AccessLevel.NONE)
    private ZonedDateTime createdAt;

    @Schema(
        description = "The last updated date/time of the Service Centre record",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    @UpdateTimestamp
    @Setter(AccessLevel.NONE)
    private ZonedDateTime lastUpdatedAt;

    @Schema(description = "The list of associated Service Area IDs")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "uuid[]")
    private List<UUID> serviceAreaIds;

    @Schema(description = "The ID of the associated Region")
    @NotNull
    @Column(name = "region_id", nullable = false)
    private UUID regionId;

    @Schema(description = "The catchment type")
    @Enumerated(EnumType.STRING)
    private CatchmentType catchmentType;

    @Override
    public UUID getAuditSubjectId() {
        return id;
    }

    @Override
    public SubjectType getAuditSubjectType() {
        return SubjectType.SERVICE_CENTRE;
    }
}
