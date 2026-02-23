package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceAreaServiceTest {

    @Mock
    private ServiceAreaRepository serviceAreaRepository;

    @InjectMocks
    private ServiceAreaService serviceAreaService;

    @Test
    void getServiceAreaByNameShouldTrimAndReturnArea() {
        ServiceArea area = new ServiceArea();
        area.setName("Money Claims");
        when(serviceAreaRepository.findByNameIgnoreCase("Money Claims"))
            .thenReturn(Optional.of(area));

        ServiceArea response = serviceAreaService.getServiceAreaByName("  Money Claims  ");

        assertThat(response).isEqualTo(area);
        verify(serviceAreaRepository).findByNameIgnoreCase("Money Claims");
    }

    @Test
    void getServiceAreaByNameShouldThrowWhenMissing() {
        when(serviceAreaRepository.findByNameIgnoreCase("Missing"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceAreaService.getServiceAreaByName("Missing"))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Service area Missing not found");
    }

    @Test
    void getAllServiceAreasForServiceShouldTrimAndReturnAreas() {
        ServiceArea area = new ServiceArea();
        area.setName("Civil");
        List<ServiceArea> results = List.of(area);
        when(serviceAreaRepository.findAllByServiceName("Family"))
            .thenReturn(results);

        List<ServiceArea> response = serviceAreaService.getAllServiceAreasForService("  Family ");

        assertThat(response).isEqualTo(results);
        verify(serviceAreaRepository).findAllByServiceName("Family");
    }

    @Test
    void getAllServiceAreasForServiceShouldThrowWhenNoneFound() {
        when(serviceAreaRepository.findAllByServiceName("Family"))
            .thenReturn(List.of());

        assertThatThrownBy(() -> serviceAreaService.getAllServiceAreasForService("Family"))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("No service areas found for service Family");
    }
}
