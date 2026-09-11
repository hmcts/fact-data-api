package uk.gov.hmcts.reform.fact.data.api.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
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
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidCourtSlug;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import javax.annotation.concurrent.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import uk.gov.hmcts.reform.fact.data.api.controllers.ServiceCentreController.ServiceCentreDetailsView;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Immutable
@JsonView(ServiceCentreDetailsView.class)
@Table(name = "service_centre")
public class ServiceCentreDetails {

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
        message = ValidationConstants.SERVICE_CENTRE_NAME_LENGTH_MESSAGE
    )
    @Pattern(
        regexp = ValidationConstants.COURT_SERVICE_CENTRE_NAME_REGEX,
        message = ValidationConstants.SERVICE_CENTRE_NAME_REGEX_MESSAGE
    )
    private String name;

    @Schema(description = "The Service Centre 'slug'")
    @ValidCourtSlug
    private String slug;

    @Schema(description = "The open status of the Service Centre")
    private Boolean open;

    @Schema(description = "Any warning notices attached to the Service Centre")
    @Size(max = ValidationConstants.COMMON_TEXT_MAX_LENGTH,
        message = ValidationConstants.WARNING_NOTICE_MAX_LENGTH_MESSAGE
    )
    @Pattern(
        regexp = ValidationConstants.WARNING_NOTICE_REGEX,
        message = ValidationConstants.WARNING_NOTICE_REGEX_MESSAGE
    )
    @Column(name = "warning_notice")
    private String warningNotice;

    @Schema(description = "Any Welsh warning notices attached to the Service Centre")
    @Size(max = ValidationConstants.COMMON_TEXT_MAX_LENGTH,
        message = ValidationConstants.WARNING_NOTICE_MAX_LENGTH_MESSAGE
    )
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
    @JsonIgnore
    private List<UUID> serviceAreaIds;

    @Schema(description = "The ID of the associated Region")
    @Column(name = "region_id")
    private UUID regionId;

    @Transient
    @JsonIgnore
    private List<ServiceArea> serviceAreaDetails;

    @JsonView(ServiceCentreDetailsView.class)
    @JsonProperty("serviceAreas")
    public List<?> getServiceAreasForView() {
        return serviceAreaDetails != null ? serviceAreaDetails : serviceAreaIds;
    }

    @Schema(description = "The catchment type")
    @Enumerated(EnumType.STRING)
    private CatchmentType catchmentType;

    @Schema(description = "The Addresses for the Service Centre")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_centre_id", insertable = false, updatable = false)
    private List<ServiceCentreAddress> serviceCentreAddresses;

    @Schema(description = "The Contact Details for the Service Centre")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_centre_id", insertable = false, updatable = false)
    private List<ServiceCentreContactDetails> serviceCentreContactDetails;

    @Schema(description = "The Areas of Law for the Service Centre")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_centre_id", insertable = false, updatable = false)
    private List<ServiceCentreAreasOfLaw> serviceCentreAreasOfLaw;
}
