package uk.gov.hmcts.reform.fact.data.api.entities;

import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

import java.util.UUID;

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

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@Table(name = "court_accessibility_options")
public class CourtAccessibilityOptions {

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
    @Size(max = ValidationConstants.PHONE_NO_MAX_LENGTH, message = ValidationConstants.PHONE_NO_MAX_LENGTH_MESSAGE)
    @Pattern(regexp = ValidationConstants.PHONE_NO_REGEX, message = ValidationConstants.PHONE_NO_REGEX_MESSAGE)
    private String accessibleParkingPhoneNumber;

    @Schema(description = "Details of available accessible toilets")
    @Size(max = 255, message = "Accessible toilet description must not exceed 255 characters")
    private String accessibleToiletDescription;

    @Schema(description = "Welsh language details of available accessible toilets")
    @Size(max = 255, message = "Welsh accessible toilet description must not exceed 255 characters")
    private String accessibleToiletDescriptionCy;

    @Schema(description = "The accessible entrance status")
    @NotNull
    private Boolean accessibleEntrance;

    @Schema(description = "The contact phone number for accessible entrance enquiries")
    @Size(max = ValidationConstants.PHONE_NO_MAX_LENGTH, message = ValidationConstants.PHONE_NO_MAX_LENGTH_MESSAGE)
    @Pattern(regexp = ValidationConstants.PHONE_NO_REGEX, message = ValidationConstants.PHONE_NO_REGEX_MESSAGE)
    private String accessibleEntrancePhoneNumber;

    @Schema(description = "Details of available hearing enhancement equipment")
    @NotNull
    @Size(max = 255, message = "Hearing Enhancement Equipment must not exceed 255 characters")
    private String hearingEnhancementEquipment;

    @Schema(description = "Lift availability status")
    @NotNull
    private Boolean lift;

    @Schema(description = "Lift door width (in cm)")
    @Min(value = 1, message = "Lift door width needs to be over 1cm")
    @Max(value = 1000, message = "Lift door width needs to be under 1000cm")
    private Integer liftDoorWidth;

    @Schema(description = "Lift weight limit (in kg)")
    @Min(value = 1, message = "Lift weight limit should be at least 1kg")
    @Max(value = 10000, message = "Lift weight limit should be at most 10000kg")
    private Integer liftDoorLimit;

    @Schema(description = "Quiet room availability status")
    @NotNull
    private Boolean quietRoom;

}
