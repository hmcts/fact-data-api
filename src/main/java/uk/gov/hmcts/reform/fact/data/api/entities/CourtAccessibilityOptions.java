package uk.gov.hmcts.reform.fact.data.api.entities;

import uk.gov.hmcts.reform.fact.data.api.audit.AuditableCourtEntityListener;
import uk.gov.hmcts.reform.fact.data.api.entities.types.HearingEnhancementEquipment;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidConditional;
import uk.gov.hmcts.reform.fact.data.api.controllers.CourtController.CourtDetailsView;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@EntityListeners(AuditableCourtEntityListener.class)
@ValidConditional(
    selected = "accessibleEntrance", selectedValueForRequired = "false", required = "accessibleEntrancePhoneNumber"
)
@ValidConditional(selected = "lift", selectedValueForRequired = "true", required = "liftDoorWidth")
@ValidConditional(selected = "lift", selectedValueForRequired = "true", required = "liftDoorLimit")
@ValidConditional(selected = "lift", selectedValueForRequired = "false", required = "liftSupportPhoneNumber")
@JsonView(CourtDetailsView.class)
@Table(name = "court_accessibility_options")
public class CourtAccessibilityOptions implements AuditableCourtEntity {

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

    @Schema(description = "The accessible parking status")
    @NotNull
    private Boolean accessibleParking;

    @Schema(description = "The contact phone number for accessible parking enquiries")
    @Size(max = ValidationConstants.PHONE_NO_MAX_LENGTH,
        message = ValidationConstants.PHONE_NO_MAX_LENGTH_MESSAGE
    )
    @Pattern(regexp = ValidationConstants.PHONE_NO_REGEX,
        message = ValidationConstants.PHONE_NO_REGEX_MESSAGE
    )
    private String accessibleParkingPhoneNumber;

    @Schema(description = "Details of available accessible toilets")
    @Size(max = ValidationConstants.ACCESSIBLE_TOILET_DESCRIPTION_MAX_LENGTH,
        message = ValidationConstants.ACCESSIBLE_TOILET_DESCRIPTION_MAX_LENGTH_MESSAGE)

    @Pattern(regexp = ValidationConstants.ENGLISH_TEXT_REGEX,
        message = ValidationConstants.ENGLISH_TEXT_REGEX_MESSAGE
    )
    private String accessibleToiletDescription;

    @Schema(description = "Welsh language details of available accessible toilets")
    @Size(max = ValidationConstants.ACCESSIBLE_TOILET_DESCRIPTION_MAX_LENGTH,
        message = ValidationConstants.WELSH_ACCESSIBLE_TOILET_DESCRIPTION_MAX_LENGTH_MESSAGE
    )
    @Pattern(regexp = ValidationConstants.WELSH_TEXT_REGEX,
        message = ValidationConstants.WELSH_TEXT_REGEX_MESSAGE
    )
    private String accessibleToiletDescriptionCy;

    @Schema(description = "The accessible entrance status")
    @NotNull
    private Boolean accessibleEntrance;

    @Schema(description = "The contact phone number for accessible entrance enquiries")
    @Size(max = ValidationConstants.PHONE_NO_MAX_LENGTH,
        message = ValidationConstants.PHONE_NO_MAX_LENGTH_MESSAGE
    )
    @Pattern(regexp = ValidationConstants.PHONE_NO_REGEX,
        message = ValidationConstants.PHONE_NO_REGEX_MESSAGE
    )
    private String accessibleEntrancePhoneNumber;

    @Schema(description = "Details of available hearing enhancement equipment")
    @NotNull
    private HearingEnhancementEquipment hearingEnhancementEquipment;

    @Schema(description = "Lift availability status")
    @NotNull
    private Boolean lift;

    @Schema(description = "Lift door width (in cm)")
    @Min(value = ValidationConstants.LIFT_DOOR_WIDTH_MIN,
        message = ValidationConstants.LIFT_DOOR_WIDTH_MIN_MESSAGE
    )
    @Max(value = ValidationConstants.LIFT_DOOR_WIDTH_MAX,
        message = ValidationConstants.LIFT_DOOR_WIDTH_MAX_MESSAGE
    )
    private Integer liftDoorWidth;

    @Schema(description = "Lift weight limit (in kg)")
    @Min(value = ValidationConstants.LIFT_WEIGHT_LIMIT_MIN,
        message = ValidationConstants.LIFT_WEIGHT_LIMIT_MIN_MESSAGE
    )
    @Max(value = ValidationConstants.LIFT_WEIGHT_LIMIT_MAX,
        message = ValidationConstants.LIFT_WEIGHT_LIMIT_MAX_MESSAGE
    )
    private Integer liftDoorLimit;

    @Schema(
        description = "Telephone number for organising support at court when there is no lift"
    )
    @Size(max = ValidationConstants.PHONE_NO_MAX_LENGTH,
        message = ValidationConstants.PHONE_NO_MAX_LENGTH_MESSAGE
    )
    @Pattern(regexp = ValidationConstants.PHONE_NO_REGEX,
        message = ValidationConstants.PHONE_NO_REGEX_MESSAGE
    )
    @Column(name = "lift_support_phone_number")
    private String liftSupportPhoneNumber;

    @Schema(description = "Quiet room availability status")
    @NotNull
    private Boolean quietRoom;

}
