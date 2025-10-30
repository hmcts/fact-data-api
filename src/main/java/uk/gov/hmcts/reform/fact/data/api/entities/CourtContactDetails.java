package uk.gov.hmcts.reform.fact.data.api.entities;

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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

import java.util.UUID;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@Table(name = "court_contact_details")
public class CourtContactDetails {

    @Schema(
        description = "The internal ID - assigned by the server during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Schema(description = "The ID of the associated Court", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Column(name = "court_id")
    private UUID courtId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    private Court court;

    @Schema(description = "The ID of the associated Contact Description Type")
    @Column(name = "court_contact_description_id")
    private UUID courtContactDescriptionId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_contact_description_id", insertable = false, updatable = false)
    private ContactDescriptionType courtContactDescription;

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

}
