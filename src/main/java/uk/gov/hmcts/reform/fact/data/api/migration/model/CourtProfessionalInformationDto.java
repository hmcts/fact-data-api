package uk.gov.hmcts.reform.fact.data.api.migration.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CourtProfessionalInformationDto(
    @JsonProperty("interview_rooms") Boolean interviewRooms,
    @JsonProperty("interview_room_count") Integer interviewRoomCount,
    @JsonProperty("interview_phone_number") String interviewPhoneNumber,
    @JsonProperty("video_hearings") Boolean videoHearings,
    @JsonProperty("common_platform") Boolean commonPlatform,
    @JsonProperty("access_scheme") Boolean accessScheme
) {
}
