package uk.gov.hmcts.reform.fact.data.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;

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
class CourtServiceTest {

    @Mock
    private CourtRepository courtRepository;
    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private CourtService courtService;

    private Court court;
    private Region region;


    @BeforeEach
    void setUp() {

        UUID regionId = UUID.randomUUID();
        region = new Region();
        region.setId(regionId);

        UUID courtId = UUID.randomUUID();
        court = new Court();
        court.setId(courtId);
        court.setRegionId(regionId);
    }

    @Test
    void create() {

        when(courtRepository.save(any(Court.class))).thenReturn(court);

        var result = courtService.create(court);

        assertNotNull(result);
        verify(courtRepository).save(court);
    }

    @Test
    void update() throws NotFoundException {
        when(courtRepository.existsById(court.getId())).thenReturn(true);
        when(courtRepository.save(any(Court.class))).thenReturn(court);
        var result = courtService.update(court);

        assertNotNull(result);
        verify(courtRepository).existsById(court.getId());
        verify(courtRepository).save(court);
    }

    @Test
    void updateFailsWithNotFoundExceptionForMissingCourt() {
        when(courtRepository.existsById(court.getId())).thenReturn(false);
        assertThrows(NotFoundException.class, () -> courtService.update(court));
    }

    @Test
    void find() throws NotFoundException {
        when(courtRepository.findById(court.getId())).thenReturn(Optional.of(court));
        var result = courtService.retrieve(court.getId());
        assertNotNull(result);
        verify(courtRepository).findById(court.getId());
    }

    @Test
    void findThrowsNotFoundExceptionForMissingCourt() {
        when(courtRepository.findById(court.getId())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> courtService.retrieve(court.getId()));
        verify(courtRepository).findById(court.getId());
    }

    @Test
    void findAll() {
        when(courtRepository.findAll()).thenReturn(List.of(court));
        var result = courtService.retrieveAll();
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(courtRepository).findAll();
    }

    @Test
    void delete() {
        courtService.delete(court.getId());
        verify(courtRepository).deleteById(court.getId());
    }
}
