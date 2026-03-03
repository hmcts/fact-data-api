package uk.gov.hmcts.reform.fact.data.api.migration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourtProfessionalInformationDto {
    @JsonProperty("interview_rooms")
    private Boolean interviewRooms;
    @JsonProperty("interview_room_count")
    private Integer interviewRoomCount;
    @JsonProperty("interview_phone_number")
    private String interviewPhoneNumber;
    @JsonProperty("video_hearings")
    private Boolean videoHearings;
    @JsonProperty("common_platform")
    private Boolean commonPlatform;
    @JsonProperty("access_scheme")
    private Boolean accessScheme;
}
