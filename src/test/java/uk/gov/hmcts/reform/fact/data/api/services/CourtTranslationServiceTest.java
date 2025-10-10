package uk.gov.hmcts.reform.fact.data.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtTranslation;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtTranslationRepository;

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
class CourtTranslationServiceTest {

    @Mock
    private CourtTranslationRepository courtTranslationRepository;
    @Mock
    private CourtRepository courtRepository;

    @InjectMocks
    private TranslationService translationService;

    private CourtTranslation courtTranslation;
    private Court court;

    @BeforeEach
    void setUp() {

        UUID courtId = UUID.randomUUID();
        court = new Court();
        court.setId(courtId);

        UUID translationId = UUID.randomUUID();
        courtTranslation = new CourtTranslation();
        courtTranslation.setId(translationId);
        courtTranslation.setCourtId(courtId);
    }

    @Test
    void create() {
        when(courtTranslationRepository.save(any(CourtTranslation.class))).thenReturn(courtTranslation);
        var result = translationService.create(courtTranslation);
        assertNotNull(result);
        verify(courtTranslationRepository).save(courtTranslation);
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
        when(courtTranslationRepository.existsById(courtTranslation.getId())).thenReturn(true);
        when(courtTranslationRepository.save(any(CourtTranslation.class))).thenReturn(courtTranslation);
        var result = translationService.update(courtTranslation);

        assertNotNull(result);
        verify(courtTranslationRepository).existsById(courtTranslation.getId());
        verify(courtTranslationRepository).save(courtTranslation);
    }

    @Test
    void updateFailsWithNotFoundExceptionForMissingTranslation() {
        when(courtTranslationRepository.existsById(courtTranslation.getId())).thenReturn(false);
        assertThrows(NotFoundException.class, () -> translationService.update(courtTranslation));
    }

    @Test
    void find() throws NotFoundException {
        when(courtTranslationRepository.findById(courtTranslation.getId())).thenReturn(Optional.of(courtTranslation));
        var result = translationService.retrieve(courtTranslation.getId());
        assertNotNull(result);
        verify(courtTranslationRepository).findById(courtTranslation.getId());
    }

    @Test
    void findThrowsNotFoundExceptionForMissingTranslation() {
        when(courtTranslationRepository.findById(courtTranslation.getId())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> translationService.retrieve(courtTranslation.getId()));
        verify(courtTranslationRepository).findById(courtTranslation.getId());
    }

    @Test
    void findAll() {
        when(courtTranslationRepository.findAll()).thenReturn(List.of(courtTranslation));
        var result = translationService.retrieveAll();
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(courtTranslationRepository).findAll();
    }

    @Test
    void delete() {
        translationService.delete(courtTranslation.getId());
        verify(courtTranslationRepository).deleteById(courtTranslation.getId());
    }
}
