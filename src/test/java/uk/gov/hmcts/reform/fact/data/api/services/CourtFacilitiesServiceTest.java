package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFacilities;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFacilitiesRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtFacilitiesServiceTest {

    @Mock
    private CourtFacilitiesRepository courtFacilitiesRepository;

    @Mock
    private CourtService courtService;

    @InjectMocks
    private CourtFacilitiesService courtFacilitiesService;

    private UUID courtId;
    private Court court;
    private CourtFacilities facilities;

    @BeforeEach
    void setup() {
        courtId = UUID.randomUUID();
        court = new Court();
        court.setId(courtId);
        court.setName("Test Court");

        facilities = new CourtFacilities();
        facilities.setParking(true);
        facilities.setParking(false);
    }

    @Test
    void getFacilitiesByCourtIdReturnsFacilitiesWhenFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtFacilitiesRepository.findByCourtId(courtId)).thenReturn(Optional.of(facilities));

        CourtFacilities result = courtFacilitiesService.getFacilitiesByCourtId(courtId);

        assertThat(result).isEqualTo(facilities);
    }

    @Test
    void getFacilitiesByCourtIdThrowsFacilitiesNotFoundWhenNotFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtFacilitiesRepository.findByCourtId(courtId)).thenReturn(Optional.empty());

        CourtResourceNotFoundException  exception = assertThrows(
            CourtResourceNotFoundException.class, () ->
            courtFacilitiesService.getFacilitiesByCourtId(courtId)
        );

        assertThat(exception.getMessage()).contains(courtId.toString());
    }

    @Test
    void getFacilitiesByCourtIdThrowsNotFoundExceptionWhenCourtNotFound() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtFacilitiesService.getFacilitiesByCourtId(courtId)
        );
    }

    @Test
    void setFacilitiesCreatesNewFacilitiesWhenNoneExists() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtFacilitiesRepository.findByCourtId(courtId)).thenReturn(Optional.empty());
        when(courtFacilitiesRepository.save(facilities)).thenReturn(facilities);

        CourtFacilities result = courtFacilitiesService.setFacilities(courtId, facilities);

        assertThat(result.getCourtId()).isEqualTo(courtId);
        verify(courtFacilitiesRepository).save(facilities);
    }

    @Test
    void setFacilitiesUpdatesExistingFacilities() {
        CourtFacilities existing = new CourtFacilities();
        existing.setId(UUID.randomUUID());
        existing.setCourtId(courtId);

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtFacilitiesRepository.findByCourtId(courtId)).thenReturn(Optional.of(existing));
        when(courtFacilitiesRepository.save(any())).thenReturn(facilities);

        CourtFacilities result = courtFacilitiesService.setFacilities(courtId, facilities);

        assertThat(result).isEqualTo(facilities);
        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(result.getCourtId()).isEqualTo(courtId);
    }

    @Test
    void setFacilitiesThrowsNotFoundExceptionWhenCourtDoesNotExist() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtFacilitiesService.setFacilities(courtId, facilities)
        );
    }
}

