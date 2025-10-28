package uk.gov.hmcts.reform.fact.data.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
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
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
public class ProfessionalInformationDto {

    private static final String INTERVIEW_ROOM_COUNT_MESSAGE =
        "Interview room count must be between 1 and 150 when interview rooms are available; otherwise omit or set to 0";

    @Schema(description = "Interview room availability status", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Boolean interviewRooms;

    @Schema(description = "Number of available interview rooms")
    @Min(0)
    @Max(150)
    private Integer interviewRoomCount;

    @Schema(description = "The phone number for interview room enquiries")
    @Size(max = ValidationConstants.PHONE_NO_MAX_LENGTH, message = ValidationConstants.PHONE_NO_MAX_LENGTH_MESSAGE)
    @Pattern(regexp = ValidationConstants.PHONE_NO_REGEX, message = ValidationConstants.PHONE_NO_REGEX_MESSAGE)
    private String interviewPhoneNumber;

    @Schema(description = "Video hearing capability status", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Boolean videoHearings;

    @Schema(description = "Common platform status", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Boolean commonPlatform;

    @Schema(description = "Access scheme status", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Boolean accessScheme;

    @AssertTrue(message = INTERVIEW_ROOM_COUNT_MESSAGE)
    public boolean isInterviewRoomCountConsistent() {
        if (Boolean.TRUE.equals(interviewRooms)) {
            return interviewRoomCount != null && interviewRoomCount > 0 && interviewRoomCount <= 150;
        }
        return interviewRoomCount == null || interviewRoomCount == 0;
    }
}
