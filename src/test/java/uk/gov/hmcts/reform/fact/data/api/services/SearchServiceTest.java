package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.Service;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceRepository;
import uk.gov.hmcts.reform.fact.data.api.services.search.SearchService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private SearchService searchService;

    @Test
    void getAllServicesShouldReturnResults() {
        Service service = new Service();
        service.setName("Civil");
        List<Service> results = List.of(service);
        when(serviceRepository.findAll()).thenReturn(results);

        List<Service> response = searchService.getAllServices();

        assertThat(response).isEqualTo(results);
        verify(serviceRepository).findAll();
    }
}
