package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

    private static final UUID REGION_ID = UUID.randomUUID();

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private RegionService regionService;

    @Test
    void getAllRegionsShouldReturnRegions() {
        Region region = new Region();
        region.setId(REGION_ID);
        region.setName("South East");

        when(regionRepository.findAll()).thenReturn(List.of(region));

        List<Region> result = regionService.getAllRegions();

        assertThat(result).containsExactly(region);
        verify(regionRepository).findAll();
    }

    @Test
    void getRegionByIdShouldReturnRegionWhenFound() {
        Region region = new Region();
        region.setId(REGION_ID);
        region.setName("Midlands");

        when(regionRepository.findById(REGION_ID)).thenReturn(Optional.of(region));

        Region result = regionService.getRegionById(REGION_ID);

        assertThat(result).isEqualTo(region);
        verify(regionRepository).findById(REGION_ID);
    }

    @Test
    void getRegionByIdShouldThrowNotFoundWhenMissing() {
        when(regionRepository.findById(REGION_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            regionService.getRegionById(REGION_ID)
        );
    }
}
