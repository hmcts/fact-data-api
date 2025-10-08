package uk.gov.hmcts.reform.fact.data.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
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
class RegionServiceTest {

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private RegionService regionService;

    private Region region;


    @BeforeEach
    void setUp() {
        UUID regionId = UUID.randomUUID();
        region = new Region();
        region.setId(regionId);
    }

    @Test
    void create() {

        when(regionRepository.save(any(Region.class))).thenReturn(region);

        var result = regionService.create(region);

        assertNotNull(result);
        verify(regionRepository).save(region);
    }

    @Test
    void checkNpesForNullFields() {
        assertThrows(NullPointerException.class, () -> regionService.create(null));
        assertThrows(NullPointerException.class, () -> regionService.update(null));
        assertThrows(NullPointerException.class, () -> regionService.retrieve(null));
        assertThrows(NullPointerException.class, () -> regionService.delete(null));
    }


    @Test
    void update() throws NotFoundException {
        when(regionRepository.existsById(region.getId())).thenReturn(true);
        when(regionRepository.save(any(Region.class))).thenReturn(region);
        var result = regionService.update(region);

        assertNotNull(result);
        verify(regionRepository).existsById(region.getId());
        verify(regionRepository).save(region);
    }

    @Test
    void updateFailsWithNotFoundExceptionForMissingRegion() {
        when(regionRepository.existsById(region.getId())).thenReturn(false);
        assertThrows(NotFoundException.class, () -> regionService.update(region));
    }

    @Test
    void find() throws NotFoundException {
        when(regionRepository.findById(region.getId())).thenReturn(Optional.of(region));
        var result = regionService.retrieve(region.getId());
        assertNotNull(result);
        verify(regionRepository).findById(region.getId());
    }

    @Test
    void findThrowsNotFoundExceptionForMissingRegion() {
        when(regionRepository.findById(region.getId())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> regionService.retrieve(region.getId()));
        verify(regionRepository).findById(region.getId());
    }

    @Test
    void findAll() {
        when(regionRepository.findAll()).thenReturn(List.of(region));
        var result = regionService.retrieveAll();
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(regionRepository).findAll();
    }

    @Test
    void delete() {
        regionService.delete(region.getId());
        verify(regionRepository).deleteById(region.getId());
    }
}
