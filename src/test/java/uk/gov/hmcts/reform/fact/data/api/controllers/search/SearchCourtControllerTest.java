package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.dto.AllLocation;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;
import uk.gov.hmcts.reform.fact.data.api.services.AllLocationService;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;
import uk.gov.hmcts.reform.fact.data.api.services.search.SearchCourtService;

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

    @Mock
    private AllLocationService allLocationService;

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
        AllLocation court = AllLocation.builder()
            .name("Alpha Court")
            .locationType("COURT")
            .serviceCentre(false)
            .build();
        AllLocation serviceCentre = AllLocation.builder()
            .name("Alpha Service Centre")
            .locationType("SERVICE_CENTRE")
            .serviceCentre(true)
            .build();
        List<AllLocation> locations = List.of(court, serviceCentre);

        when(allLocationService.getOpenLocationsByPrefix("A")).thenReturn(locations);

        ResponseEntity<List<AllLocation>> response = controller.getCourtsByPrefix("A");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(locations);
        verify(allLocationService).getOpenLocationsByPrefix("A");
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
