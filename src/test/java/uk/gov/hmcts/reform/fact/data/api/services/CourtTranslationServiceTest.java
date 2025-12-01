package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtTranslation;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtTranslationRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtTranslationServiceTest {

    @Mock
    private CourtTranslationRepository courtTranslationRepository;

    @Mock
    private CourtService courtService;

    @InjectMocks
    private CourtTranslationService courtTranslationService;

    private UUID courtId;
    private Court court;
    private CourtTranslation translation;

    @BeforeEach
    void setup() {
        courtId = UUID.randomUUID();
        court = new Court();
        court.setId(courtId);
        court.setName("Test Court");

        translation = new CourtTranslation();
        translation.setEmail("test@example.com");
        translation.setPhoneNumber("1234567890");
    }

    @Test
    void getTranslationByCourtIdReturnsTranslationWhenFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtTranslationRepository.findByCourtId(courtId)).thenReturn(Optional.of(translation));

        CourtTranslation result = courtTranslationService.getTranslationByCourtId(courtId);

        assertThat(result).isEqualTo(translation);
    }

    @Test
    void getTranslationByCourtIdThrowsCourtResourceNotFoundWhenNotFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtTranslationRepository.findByCourtId(courtId)).thenReturn(Optional.empty());

        CourtResourceNotFoundException exception = assertThrows(CourtResourceNotFoundException.class, () ->
            courtTranslationService.getTranslationByCourtId(courtId)
        );

        assertThat(exception.getMessage()).contains(courtId.toString());
    }

    @Test
    void getTranslationByCourtIdThrowsNotFoundExceptionWhenCourtNotFound() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtTranslationService.getTranslationByCourtId(courtId)
        );
    }

    @Test
    void setTranslationCreatesNewTranslationWhenNoneExists() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtTranslationRepository.findByCourtId(courtId)).thenReturn(Optional.empty());
        when(courtTranslationRepository.save(translation)).thenReturn(translation);

        CourtTranslation result = courtTranslationService.setTranslation(courtId, translation);

        assertThat(result.getCourtId()).isEqualTo(courtId);
        verify(courtTranslationRepository).save(translation);
    }

    @Test
    void setTranslationUpdatesExistingTranslation() {
        CourtTranslation existing = new CourtTranslation();
        existing.setId(UUID.randomUUID());
        existing.setCourtId(courtId);

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtTranslationRepository.findByCourtId(courtId)).thenReturn(Optional.of(existing));
        when(courtTranslationRepository.save(any())).thenReturn(translation);

        CourtTranslation result = courtTranslationService.setTranslation(courtId, translation);

        assertThat(result).isEqualTo(translation);
        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(result.getCourtId()).isEqualTo(courtId);
    }

    @Test
    void setTranslationThrowsNotFoundExceptionWhenCourtDoesNotExist() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtTranslationService.setTranslation(courtId, translation)
        );
    }
}
