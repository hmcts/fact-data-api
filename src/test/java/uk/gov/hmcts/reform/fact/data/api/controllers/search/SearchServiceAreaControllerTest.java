package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceAreas;
import uk.gov.hmcts.reform.fact.data.api.services.CourtServiceAreaService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceAreaControllerTest {

    @Mock
    private CourtServiceAreaService courtServiceAreaService;

    @InjectMocks
    private SearchServiceAreaController controller;

    @Test
    void getServiceAreaByNameShouldReturnOk() {
        CourtServiceAreas courtServiceAreas = new CourtServiceAreas();
        List<CourtServiceAreas> results = List.of(courtServiceAreas);

        when(courtServiceAreaService.findByServiceAreaName("Money Claims"))
            .thenReturn(results);

        ResponseEntity<List<CourtServiceAreas>> response = controller.getServiceAreaByName("Money Claims");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(results);
        verify(courtServiceAreaService).findByServiceAreaName("Money Claims");
    }
}
