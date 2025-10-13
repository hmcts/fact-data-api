package uk.gov.hmcts.reform.fact.data.api.entities;

import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "court_accessibility_options")
public class CourtAccessibilityOption extends BaseCourtEntity {

    @Schema(description = "The accessible parking status")
    @NotNull
    private Boolean accessibleParking;

    @Schema(description = "The contact phone number for accessible parking enquiries")
    @Size(max = ValidationConstants.PHONE_NO_MAX_LENGTH, message = ValidationConstants.PHONE_NO_MAX_LENGTH_MESSAGE)
    @Pattern(regexp = ValidationConstants.PHONE_NO_REGEX, message = ValidationConstants.PHONE_NO_REGEX_MESSAGE)
    private String accessibleParkingPhoneNumber;

    @Schema(description = "Details of available accessible toilets")
    private String accessibleToiletDescription;

    @Schema(description = "Welsh language details of available accessible toilets")
    private String accessibleToiletDescriptionCy;

    @Schema(description = "The accessible entrance status")
    @NotNull
    private Boolean accessibleEntrance;

    @Schema(description = "The contact phone number for accessible entrance enquiries")
    @Size(max = ValidationConstants.PHONE_NO_MAX_LENGTH, message = ValidationConstants.PHONE_NO_MAX_LENGTH_MESSAGE)
    @Pattern(regexp = ValidationConstants.PHONE_NO_REGEX, message = ValidationConstants.PHONE_NO_REGEX_MESSAGE)
    private String accessibleEntrancePhoneNumber;

    @Schema(description = "Details of available hearing enhancement equipment")
    private String hearingEnhancementEquipment;

    @Schema(description = "Lift availability status")
    @NotNull
    private Boolean lift;

    @Schema(description = "Lift door width (in cm)")
    @Min(value = 100, message = "Lift door width needs to be over 100cm")
    private Integer liftDoorWidth;

    @Schema(description = "Lift weight limit (in kg)")
    @Min(value = 150, message = "Lift weight limit should be at least 150kg")
    @Max(value = 3000, message = "Lift weight limit should be at most 3000kg")
    private Integer liftDoorLimit;

    @Schema(description = "Quiet room availability status")
    @NotNull
    private Boolean quietRoom;

}
