package uk.gov.hmcts.reform.fact.data.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Translation;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.TranslationRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private TranslationRepository translationRepository;
    @Mock
    private CourtRepository courtRepository;

    @InjectMocks
    private TranslationService translationService;

    private Translation translation;
    private Court court;

    @BeforeEach
    void setUp() {

        UUID courtId = UUID.randomUUID();
        court = new Court();
        court.setId(courtId);

        UUID translationId = UUID.randomUUID();
        translation = new Translation();
        translation.setId(translationId);
        translation.setCourtId(courtId);
    }

    @Test
    void create() {
        when(translationRepository.save(any(Translation.class))).thenReturn(translation);
        var result = translationService.create(translation);
        assertNotNull(result);
        verify(translationRepository).save(translation);
    }

    @Test
    void checkNpesForNullFields() {
        assertThrows(NullPointerException.class, () -> translationService.create(null));
        assertThrows(NullPointerException.class, () -> translationService.update(null));
        assertThrows(NullPointerException.class, () -> translationService.retrieve(null));
        assertThrows(NullPointerException.class, () -> translationService.delete(null));
    }

    @Test
    void update() throws NotFoundException {
        when(translationRepository.existsById(translation.getId())).thenReturn(true);
        when(translationRepository.save(any(Translation.class))).thenReturn(translation);
        var result = translationService.update(translation);

        assertNotNull(result);
        verify(translationRepository).existsById(translation.getId());
        verify(translationRepository).save(translation);
    }

    @Test
    void updateFailsWithNotFoundExceptionForMissingTranslation() {
        when(translationRepository.existsById(translation.getId())).thenReturn(false);
        assertThrows(NotFoundException.class, () -> translationService.update(translation));
    }

    @Test
    void find() throws NotFoundException {
        when(translationRepository.findById(translation.getId())).thenReturn(Optional.of(translation));
        var result = translationService.retrieve(translation.getId());
        assertNotNull(result);
        verify(translationRepository).findById(translation.getId());
    }

    @Test
    void findThrowsNotFoundExceptionForMissingTranslation() {
        when(translationRepository.findById(translation.getId())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> translationService.retrieve(translation.getId()));
        verify(translationRepository).findById(translation.getId());
    }

    @Test
    void findAll() {
        when(translationRepository.findAll()).thenReturn(List.of(translation));
        var result = translationService.retrieveAll();
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(translationRepository).findAll();
    }

    @Test
    void delete() {
        translationService.delete(translation.getId());
        verify(translationRepository).deleteById(translation.getId());
    }
}
