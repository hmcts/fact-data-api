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
    @Size(min = 5, max = 200, message = "Service centre name should be between 5 and 200 chars")
    @Pattern(
        regexp = "^[A-Za-z&'()\\- ]+$",
        message = "Service centre name may only contain letters, spaces, apostrophes, hyphens, ampersands, "
            + "and parentheses"
    )
    private String name;

    @Schema(description = "The Service Centre 'slug'")
    @ValidCourtSlug
    private String slug;

    @Schema(description = "The open status of the Service Centre")
    private Boolean open;

    @Schema(description = "Any warning notices attached to the Service Centre")
    @Size(max = 500, message = "Warning notice must be less than 500 characters")
    @Pattern(
        regexp = "^[A-Za-z0-9.,!?:;'\"()\\-/&@+\\s]+$",
        message = "Warning notice may only contain letters, numbers, spaces, and standard punctuation or symbols (@, +)"
    )
    @Column(name = "warning_notice")
    private String warningNotice;

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
