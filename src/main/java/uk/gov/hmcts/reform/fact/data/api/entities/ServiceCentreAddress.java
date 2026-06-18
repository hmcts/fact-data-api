package uk.gov.hmcts.reform.fact.data.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.audit.AuditableCourtEntityListener;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AuditSubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AddressType;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidPostcode;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@EntityListeners(AuditableCourtEntityListener.class)
@Table(name = "service_centre_address")
public class ServiceCentreAddress implements AuditableEntity {

    @Schema(
        description = "The internal ID - assigned by the server during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Schema(description = "The ID of the associated Service Centre", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Column(name = "service_centre_id")
    private UUID serviceCentreId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_centre_id", insertable = false, updatable = false)
    private ServiceCentre serviceCentre;

    @Schema(description = "The first address line")
    @Size(max = 255, message = "Address line should be 255 characters or less")
    @Pattern(regexp = ValidationConstants.OPTIONAL_ADDRESS_LINE_REGEX,
        message = "Address Line 1: " + ValidationConstants.ADDRESS_LINE_REGEX_MESSAGE)
    @Column(name = "address_line_1")
    private String addressLine1;

    @Schema(description = "The second address line")
    @Size(max = 255, message = "Address line should be 255 characters or less")
    @Pattern(regexp = ValidationConstants.OPTIONAL_ADDRESS_LINE_REGEX,
        message = "Address Line 2: " + ValidationConstants.ADDRESS_LINE_REGEX_MESSAGE)
    @Column(name = "address_line_2")
    private String addressLine2;

    @Schema(description = "The town/city")
    @Size(max = 100, message = "Town/City name should be 100 characters or less")
    @Pattern(regexp = ValidationConstants.OPTIONAL_ADDRESS_LINE_REGEX,
        message = "Town/City: " + ValidationConstants.ADDRESS_LINE_REGEX_MESSAGE)
    private String townCity;

    @Schema(description = "The county")
    @Size(max = 100, message = "County name should be 100 characters or less")
    @Pattern(regexp = ValidationConstants.OPTIONAL_ADDRESS_LINE_REGEX,
        message = "County: " + ValidationConstants.ADDRESS_LINE_REGEX_MESSAGE)
    private String county;

    @Schema(description = "The postcode")
    @ValidPostcode
    private String postcode;

    @Schema(description = "The latitude coordinate")
    private BigDecimal lat;

    @Schema(description = "The longitude coordinate")
    private BigDecimal lon;

    @Schema(description = "The address type")
    @Enumerated(EnumType.STRING)
    private AddressType addressType;

    @Override
    public UUID getAuditSubjectId() {
        return serviceCentreId;
    }

    @Override
    public AuditSubjectType getAuditSubjectType() {
        return AuditSubjectType.SERVICE_CENTRE;
    }
}
