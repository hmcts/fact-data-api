package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.dto.ServiceAreaSearchResult;
import uk.gov.hmcts.reform.fact.data.api.services.search.SearchServiceAreaService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceAreaControllerTest {

    @Mock
    private SearchServiceAreaService searchServiceAreaService;

    @InjectMocks
    private SearchServiceAreaController searchServiceAreaController;

    @Test
    void getServiceAreaByNameReturns200() {
        List<ServiceAreaSearchResult> results =
            List.of(ServiceAreaSearchResult.builder().id(UUID.randomUUID()).build());
        when(searchServiceAreaService.findByServiceAreaName("family")).thenReturn(results);

        ResponseEntity<List<ServiceAreaSearchResult>> response =
            searchServiceAreaController.getServiceAreaByName("family");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(results);
        verify(searchServiceAreaService).findByServiceAreaName("family");
    }
}

