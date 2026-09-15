package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.dto.SearchResult;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;
import uk.gov.hmcts.reform.fact.data.api.services.search.SearchLocationService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchLocationControllerTest {

    @Mock
    private SearchLocationService searchLocationService;

    @InjectMocks
    private SearchLocationController searchLocationController;

    @Test
    void getLocationsByPostcodeReturns200() {
        List<SearchResult> results = List.of(SearchResult.builder().id(UUID.randomUUID()).name("Court A").build());
        when(searchLocationService.getLocationsBySearchParameters("SW1A 1AA", "Divorce", SearchAction.NEAREST, 10))
            .thenReturn(results);

        ResponseEntity<List<SearchResult>> response = searchLocationController.getLocationsByPostcode(
            "SW1A 1AA",
            "Divorce",
            SearchAction.NEAREST,
            10
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(results);
        verify(searchLocationService).getLocationsBySearchParameters("SW1A 1AA", "Divorce", SearchAction.NEAREST, 10);
    }
}

