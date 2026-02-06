package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceAreas;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtServiceAreaServiceTest {

    @Mock
    private CourtServiceAreasRepository courtServiceAreasRepository;

    @Mock
    private ServiceAreaService serviceAreaService;

    @InjectMocks
    private CourtServiceAreaService courtServiceAreaService;

    @Test
    void findByServiceAreaIdShouldReturnResults() {
        UUID serviceAreaId = UUID.randomUUID();
        List<CourtServiceAreas> results = List.of(new CourtServiceAreas());
        when(courtServiceAreasRepository.findByServiceAreaId(serviceAreaId)).thenReturn(results);

        List<CourtServiceAreas> response = courtServiceAreaService.findByServiceAreaId(serviceAreaId);

        assertThat(response).isEqualTo(results);
        verify(courtServiceAreasRepository).findByServiceAreaId(serviceAreaId);
    }

    @Test
    void findByServiceAreaNameShouldResolveServiceAreaAndReturnResults() {
        UUID serviceAreaId = UUID.randomUUID();
        ServiceArea area = new ServiceArea();
        area.setId(serviceAreaId);
        List<CourtServiceAreas> results = List.of(new CourtServiceAreas());

        when(serviceAreaService.getServiceAreaByName("Money Claims")).thenReturn(area);
        when(courtServiceAreasRepository.findByServiceAreaId(serviceAreaId)).thenReturn(results);

        List<CourtServiceAreas> response = courtServiceAreaService.findByServiceAreaName("Money Claims");

        assertThat(response).isEqualTo(results);
        verify(serviceAreaService).getServiceAreaByName("Money Claims");
        verify(courtServiceAreasRepository).findByServiceAreaId(serviceAreaId);
    }
}
