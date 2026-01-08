package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;
import uk.gov.hmcts.reform.fact.data.api.services.SearchCourtService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchCourtControllerTest {

    @Mock
    private SearchCourtService searchCourtService;

    @Mock
    private CourtService courtService;

    @InjectMocks
    private SearchCourtController controller;

    @Test
    void getCourtsByPostcodeShouldReturnOk() {
        CourtWithDistance court = mock(CourtWithDistance.class);
        List<CourtWithDistance> results = List.of(court);

        when(searchCourtService.getCourtsBySearchParameters(
            "SW1A 1AA",
            "Money Claims",
            SearchAction.NEAREST,
            5
        )).thenReturn(results);

        ResponseEntity<List<CourtWithDistance>> response =
            controller.getCourtsByPostcode("SW1A 1AA", "Money Claims", SearchAction.NEAREST, 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(results);
        verify(searchCourtService).getCourtsBySearchParameters("SW1A 1AA", "Money Claims", SearchAction.NEAREST, 5);
    }

    @Test
    void getCourtsByPostcodeShouldAllowNullOptionalParams() {
        List<CourtWithDistance> results = List.of(mock(CourtWithDistance.class));
        when(searchCourtService.getCourtsBySearchParameters("SW1A 1AA", null, null, 10))
            .thenReturn(results);

        ResponseEntity<List<CourtWithDistance>> response =
            controller.getCourtsByPostcode("SW1A 1AA", null, null, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(results);
        verify(searchCourtService).getCourtsBySearchParameters("SW1A 1AA", null, null, 10);
    }

    @Test
    void getCourtsByPrefixShouldReturnOk() {
        Court court = new Court();
        court.setName("Alpha Court");
        List<Court> courts = List.of(court);

        when(courtService.getCourtsByPrefixAndActiveSearch("A")).thenReturn(courts);

        ResponseEntity<List<Court>> response = controller.getCourtsByPrefix("A");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(courts);
        verify(courtService).getCourtsByPrefixAndActiveSearch("A");
    }

    @Test
    void getCourtsByQueryShouldReturnOk() {
        Court court = new Court();
        court.setName("Example Court");
        List<Court> courts = List.of(court);

        when(courtService.searchOpenCourtsByNameOrAddress("Example")).thenReturn(courts);

        ResponseEntity<List<Court>> response = controller.getCourtsByQuery("Example");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(courts);
        verify(courtService).searchOpenCourtsByNameOrAddress("Example");
    }
}
