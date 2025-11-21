package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAccessibilityOptions;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAccessibilityOptionsRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtAccessibilityOptionsServiceTest {

    @Mock
    private CourtAccessibilityOptionsRepository courtAccessibilityOptionsRepository;

    @Mock
    private CourtService courtService;

    @InjectMocks
    private CourtAccessibilityOptionsService courtAccessibilityOptionsService;

    private UUID courtId;
    private Court court;
    private CourtAccessibilityOptions accessibilityOptions;

    @BeforeEach
    void setup() {
        courtId = UUID.randomUUID();
        court = new Court();
        court.setId(courtId);
        court.setName("Test Court");

        accessibilityOptions = new CourtAccessibilityOptions();
        accessibilityOptions.setCourtId(courtId);
        accessibilityOptions.setAccessibleParking(true);
    }

    @Test
    void getAccessibilityOptionsByCourtIdReturnsAccessibilityOptionsWhenFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAccessibilityOptionsRepository.findByCourtId(courtId)).thenReturn(Optional.of(accessibilityOptions));

        CourtAccessibilityOptions result = courtAccessibilityOptionsService.getAccessibilityOptionsByCourtId(courtId);

        assertThat(result).isEqualTo(accessibilityOptions);
    }

    @Test
    void getAccessibilityOptionsByCourtIdThrowsAccessibilityOptionsNotFoundWhenNotFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAccessibilityOptionsRepository.findByCourtId(courtId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            courtAccessibilityOptionsService.getAccessibilityOptionsByCourtId(courtId)
        );

        assertThat(exception.getMessage()).contains(courtId.toString());
    }

    @Test
    void getAccessibilityOptionsByCourtIdThrowsNotFoundExceptionWhenCourtNotFound() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtAccessibilityOptionsService.getAccessibilityOptionsByCourtId(courtId)
        );
    }

    @Test
    void setAccessibilityOptionsCreatesNewAccessibilityOptionsWhenNoneExists() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAccessibilityOptionsRepository.findByCourtId(courtId)).thenReturn(Optional.empty());
        when(courtAccessibilityOptionsRepository.save(accessibilityOptions)).thenReturn(accessibilityOptions);

        CourtAccessibilityOptions result = courtAccessibilityOptionsService
            .setAccessibilityOptions(courtId, accessibilityOptions);

        assertThat(result.getCourtId()).isEqualTo(courtId);
        verify(courtAccessibilityOptionsRepository).save(accessibilityOptions);
    }

    @Test
    void setAccessibilityOptionsUpdatesExistingAccessibilityOptions() {
        CourtAccessibilityOptions existing = new CourtAccessibilityOptions();
        existing.setId(UUID.randomUUID());
        existing.setCourtId(courtId);

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAccessibilityOptionsRepository.findByCourtId(courtId)).thenReturn(Optional.of(existing));
        when(courtAccessibilityOptionsRepository.save(any())).thenReturn(accessibilityOptions);

        CourtAccessibilityOptions result = courtAccessibilityOptionsService
            .setAccessibilityOptions(courtId, accessibilityOptions);

        assertThat(result).isEqualTo(accessibilityOptions);
        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(result.getCourtId()).isEqualTo(courtId);
    }

    @Test
    void setAccessibilityOptionsThrowsNotFoundExceptionWhenCourtDoesNotExist() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtAccessibilityOptionsService.setAccessibilityOptions(courtId, accessibilityOptions)
        );
    }
}

