package uk.gov.hmcts.reform.fact.data.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

import java.util.UUID;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@EntityListeners(AuditableCourtEntityListener.class)
@Table(name = "service_centre_contact_details")
public class ServiceCentreContactDetails implements AuditableEntity {

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

    @Schema(description = "The ID of the associated Contact Description Type")
    @Column(name = "service_centre_contact_description_id")
    private UUID serviceCentreContactDescriptionId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_centre_contact_description_id", insertable = false, updatable = false)
    private ContactDescriptionType serviceCentreContactDescription;

    @Transient
    @JsonIgnore
    private ContactDescriptionType serviceCentreContactDescriptionDetails;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("serviceCentreContactDescription")
    public ContactDescriptionType getServiceCentreContactDescriptionForView() {
        return serviceCentreContactDescriptionDetails;
    }

    @Schema(description = "The explanation")
    @Column(name = "explanation", length = 250)
    @Size(max = 250, message = "Explanation should be no more than {max} characters")
    @Pattern(
        regexp = "^[A-Za-z0-9 '\\-()&+]*$",
        message = "Explanation contains invalid characters. Allowed: letters, numbers, spaces, apostrophes, - ( ) & +"
    )
    private String explanation;

    @Schema(description = "The Welsh language explanation")
    @Column(name = "explanation_cy", length = 250)
    @Size(max = 250, message = "Explanation should be no more than {max} characters")
    @Pattern(
        regexp = "^[\\p{L}0-9 '\\-()&+]*$",
        message = "Welsh explanation contains invalid characters. Allowed: letters (with accents), numbers, spaces, "
            + "apostrophes, - ( ) & +"
    )
    private String explanationCy;

    @Schema(description = "The associated email address")
    @Size(max = ValidationConstants.EMAIL_MAX_LENGTH, message = ValidationConstants.EMAIL_MAX_LENGTH_MESSAGE)
    @Pattern(regexp = ValidationConstants.EMAIL_REGEX, message = ValidationConstants.EMAIL_REGEX_MESSAGE)
    private String email;

    @Schema(description = "The associated phone number")
    @Size(max = ValidationConstants.PHONE_NO_MAX_LENGTH, message = ValidationConstants.PHONE_NO_MAX_LENGTH_MESSAGE)
    @Pattern(regexp = ValidationConstants.PHONE_NO_REGEX, message = ValidationConstants.PHONE_NO_REGEX_MESSAGE)
    private String phoneNumber;

    @Override
    public UUID getAuditSubjectId() {
        return serviceCentreId;
    }

    @Override
    public AuditSubjectType getAuditSubjectType() {
        return AuditSubjectType.SERVICE_CENTRE;
    }
}
