package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtProfessionalInformation;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.ProfessionalInformationNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtProfessionalInformationRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtProfessionalInformationServiceTest {

    @Mock
    private CourtProfessionalInformationRepository professionalInformationRepository;

    @Mock
    private CourtService courtService;

    @InjectMocks
    private CourtProfessionalInformationService service;

    @Captor
    private ArgumentCaptor<CourtProfessionalInformation> professionalInformationCaptor;

    private UUID courtId;
    private Court court;

    @BeforeEach
    void setUp() {
        courtId = UUID.randomUUID();
        court = new Court();
        court.setId(courtId);
        court.setName("Test Court");
        court.setSlug("test-court");
        court.setRegionId(UUID.randomUUID());
    }

    @Test
    void shouldReturnProfessionalInformationWhenPresent() {
        CourtProfessionalInformation professionalInformation = new CourtProfessionalInformation();
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(professionalInformationRepository.findByCourtId(courtId))
            .thenReturn(Optional.of(professionalInformation));

        CourtProfessionalInformation result = service.getProfessionalInformation(courtId);

        assertThat(result).isSameAs(professionalInformation);
    }

    @Test
    void shouldThrowWhenProfessionalInformationMissing() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(professionalInformationRepository.findByCourtId(courtId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfessionalInformation(courtId))
            .isInstanceOf(ProfessionalInformationNotFoundException.class)
            .hasMessageContaining(courtId.toString());
    }

    @Test
    void shouldUpdateExistingProfessionalInformation() {
        UUID existingId = UUID.randomUUID();
        CourtProfessionalInformation existing = new CourtProfessionalInformation();
        existing.setId(existingId);
        existing.setCourtId(courtId);

        CourtProfessionalInformation request = new CourtProfessionalInformation();
        request.setInterviewRooms(Boolean.TRUE);
        request.setInterviewRoomCount(3);
        request.setInterviewPhoneNumber("01234 567890");
        request.setVideoHearings(Boolean.TRUE);
        request.setCommonPlatform(Boolean.TRUE);
        request.setAccessScheme(Boolean.FALSE);

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(professionalInformationRepository.findByCourtId(courtId)).thenReturn(Optional.of(existing));
        when(professionalInformationRepository.save(any(CourtProfessionalInformation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CourtProfessionalInformation result = service.setProfessionalInformation(courtId, request);

        verify(professionalInformationRepository).save(professionalInformationCaptor.capture());
        CourtProfessionalInformation saved = professionalInformationCaptor.getValue();

        assertThat(saved.getId()).isEqualTo(existingId);
        assertThat(saved.getCourtId()).isEqualTo(courtId);
        assertThat(saved.getCourt()).isEqualTo(court);
        assertThat(result).isSameAs(saved);
    }

    @Test
    void shouldCreateProfessionalInformationWhenNoneExists() {
        CourtProfessionalInformation request = new CourtProfessionalInformation();
        request.setInterviewRooms(Boolean.TRUE);
        request.setInterviewRoomCount(4);
        request.setInterviewPhoneNumber("01234 567890");
        request.setVideoHearings(Boolean.TRUE);
        request.setCommonPlatform(Boolean.FALSE);
        request.setAccessScheme(Boolean.TRUE);

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(professionalInformationRepository.findByCourtId(courtId)).thenReturn(Optional.empty());
        when(professionalInformationRepository.save(any(CourtProfessionalInformation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CourtProfessionalInformation result = service.setProfessionalInformation(courtId, request);

        verify(professionalInformationRepository).save(professionalInformationCaptor.capture());
        CourtProfessionalInformation saved = professionalInformationCaptor.getValue();

        assertThat(saved.getId()).isNull();
        assertThat(saved.getCourtId()).isEqualTo(courtId);
        assertThat(saved.getCourt()).isEqualTo(court);
        assertThat(result).isSameAs(saved);
    }
}
