package uk.gov.hmcts.reform.fact.data.api.dto;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtProfessionalInformation;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalInformationDtoTest {

    @Test
    void interviewRoomCountIsValidWhenInterviewRoomsAreAvailable() {
        ProfessionalInformationDto dto = ProfessionalInformationDto.builder()
            .interviewRooms(true)
            .interviewRoomCount(4)
            .build();

        assertThat(dto.isInterviewRoomCountConsistent()).isTrue();
    }

    @Test
    void interviewRoomCountMustBePositiveWhenInterviewRoomsAreAvailable() {
        ProfessionalInformationDto dto = ProfessionalInformationDto.builder()
            .interviewRooms(true)
            .interviewRoomCount(0)
            .build();

        assertThat(dto.isInterviewRoomCountConsistent()).isFalse();
    }

    @Test
    void interviewRoomCountCannotBeMissingOrTooHighWhenInterviewRoomsAreAvailable() {
        ProfessionalInformationDto withNullCount = ProfessionalInformationDto.builder()
            .interviewRooms(true)
            .interviewRoomCount(null)
            .build();

        ProfessionalInformationDto withTooHighCount = ProfessionalInformationDto.builder()
            .interviewRooms(true)
            .interviewRoomCount(151)
            .build();

        assertThat(withNullCount.isInterviewRoomCountConsistent()).isFalse();
        assertThat(withTooHighCount.isInterviewRoomCountConsistent()).isFalse();
    }

    @Test
    void interviewRoomCountMustBeMissingOrZeroWhenInterviewRoomsAreNotAvailable() {
        ProfessionalInformationDto withNullCount = ProfessionalInformationDto.builder()
            .interviewRooms(false)
            .interviewRoomCount(null)
            .build();

        ProfessionalInformationDto withPositiveCount = ProfessionalInformationDto.builder()
            .interviewRooms(false)
            .interviewRoomCount(1)
            .build();

        assertThat(withNullCount.isInterviewRoomCountConsistent()).isTrue();
        assertThat(withPositiveCount.isInterviewRoomCountConsistent()).isFalse();
    }

    @Test
    void interviewRoomCountCanBeZeroWhenInterviewRoomsAreNotAvailable() {
        ProfessionalInformationDto withZeroCount = ProfessionalInformationDto.builder()
            .interviewRooms(false)
            .interviewRoomCount(0)
            .build();

        assertThat(withZeroCount.isInterviewRoomCountConsistent()).isTrue();
    }

    @Test
    void fromEntityMapsFields() {
        CourtProfessionalInformation entity = CourtProfessionalInformation.builder()
            .interviewRooms(true)
            .interviewRoomCount(2)
            .interviewPhoneNumber("01234 567890")
            .videoHearings(true)
            .commonPlatform(false)
            .accessScheme(true)
            .build();

        ProfessionalInformationDto dto = ProfessionalInformationDto.fromEntity(entity);

        assertThat(dto.getInterviewRooms()).isTrue();
        assertThat(dto.getInterviewRoomCount()).isEqualTo(2);
        assertThat(dto.getInterviewPhoneNumber()).isEqualTo("01234 567890");
        assertThat(dto.getVideoHearings()).isTrue();
        assertThat(dto.getCommonPlatform()).isFalse();
        assertThat(dto.getAccessScheme()).isTrue();
    }
}

