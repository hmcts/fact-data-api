package uk.gov.hmcts.reform.fact.data.api.entities;

import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "court_contact_details")
public class CourtContactDetail extends BaseCourtEntity {

    @Schema(description = "The ID of the associated Contact Description Type")
    @Column(name = "court_contact_description_id")
    private UUID courtContactDescriptionId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_contact_description_id", insertable = false, updatable = false)
    private ContactDescriptionType courtContactDescription;

    @Schema(description = "The explanation")
    private String explanation;

    @Schema(description = "The Welsh language explanation")
    private String explanationCy;

    @Schema(description = "The associated email address")
    @Size(max = ValidationConstants.EMAIL_MAX_LENGTH, message = ValidationConstants.EMAIL_MAX_LENGTH_MESSAGE)
    @Pattern(regexp = ValidationConstants.EMAIL_REGEX, message = ValidationConstants.EMAIL_REGEX_MESSAGE)
    private String email;

    @Schema(description = "The associated phone number")
    @Size(max = ValidationConstants.PHONE_NO_MAX_LENGTH, message = ValidationConstants.PHONE_NO_MAX_LENGTH_MESSAGE)
    @Pattern(regexp = ValidationConstants.PHONE_NO_REGEX, message = ValidationConstants.PHONE_NO_REGEX_MESSAGE)
    private String phoneNumber;

}
