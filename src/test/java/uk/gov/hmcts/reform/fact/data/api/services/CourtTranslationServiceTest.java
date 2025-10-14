package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Translation;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.TranslationNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.TranslationRepository;

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
    private TranslationRepository translationRepository;

    @Mock
    private CourtService courtService;

    @InjectMocks
    private TranslationService translationService;

    private UUID courtId;
    private Court court;
    private Translation translation;

    @BeforeEach
    void setup() {
        courtId = UUID.randomUUID();
        court = new Court();
        court.setId(courtId);
        court.setName("Test Court");

        translation = new Translation();
        translation.setEmail("test@example.com");
        translation.setPhoneNumber("1234567890");
    }

    @Test
    void getTranslationByCourtIdReturnsTranslationWhenFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(translationRepository.findByCourtId(courtId)).thenReturn(Optional.of(translation));

        Translation result = translationService.getTranslationByCourtId(courtId);

        assertThat(result).isEqualTo(translation);
    }

    @Test
    void getTranslationByCourtIdThrowsTranslationNotFoundWhenNotFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(translationRepository.findByCourtId(courtId)).thenReturn(Optional.empty());

        TranslationNotFoundException exception = assertThrows(TranslationNotFoundException.class, () ->
            translationService.getTranslationByCourtId(courtId)
        );

        assertThat(exception.getMessage()).contains(courtId.toString());
    }

    @Test
    void getTranslationByCourtIdThrowsNotFoundExceptionWhenCourtNotFound() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            translationService.getTranslationByCourtId(courtId)
        );
    }

    @Test
    void setTranslationCreatesNewTranslationWhenNoneExists() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(translationRepository.findByCourtId(courtId)).thenReturn(Optional.empty());
        when(translationRepository.save(translation)).thenReturn(translation);

        Translation result = translationService.setTranslation(courtId, translation);

        assertThat(result.getCourtId()).isEqualTo(courtId);
        verify(translationRepository).save(translation);
    }

    @Test
    void setTranslationUpdatesExistingTranslation() {
        Translation existing = new Translation();
        existing.setId(UUID.randomUUID());
        existing.setCourtId(courtId);

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(translationRepository.findByCourtId(courtId)).thenReturn(Optional.of(existing));
        when(translationRepository.save(any())).thenReturn(translation);

        Translation result = translationService.setTranslation(courtId, translation);

        assertThat(result).isEqualTo(translation);
        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(result.getCourtId()).isEqualTo(courtId);
    }

    @Test
    void setTranslationThrowsNotFoundExceptionWhenCourtDoesNotExist() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            translationService.setTranslation(courtId, translation)
        );
    }
}

