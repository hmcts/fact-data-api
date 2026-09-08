package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.dto.AllLocation;
import uk.gov.hmcts.reform.fact.data.api.dto.AllLocationDetails;
import uk.gov.hmcts.reform.fact.data.api.services.AllLocationService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllLocationControllerTest {

    @Mock
    private AllLocationService allLocationService;

    @InjectMocks
    private AllLocationController allLocationController;

    @Test
    void getFilteredAndPaginatedLocationsReturns200() {
        Page<AllLocation> locations = new PageImpl<>(List.of(AllLocation.builder().id(UUID.randomUUID()).build()));
        when(allLocationService.getFilteredAndPaginatedLocations(0, 25, true, false, null, "Court", "name", "asc"))
            .thenReturn(locations);

        ResponseEntity<Page<AllLocation>> response = allLocationController.getFilteredAndPaginatedLocations(
            0,
            25,
            true,
            false,
            null,
            "Court",
            "name",
            "asc"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(locations);
        verify(allLocationService).getFilteredAndPaginatedLocations(0, 25, true, false, null, "Court", "name", "asc");
    }

    @Test
    void getAllLocationDetailsReturns200() {
        List<AllLocationDetails> details = List.of(AllLocationDetails.builder().locationType("COURT").build());
        when(allLocationService.getAllLocationDetails()).thenReturn(details);

        ResponseEntity<List<AllLocationDetails>> response = allLocationController.getAllLocationDetails();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(details);
        verify(allLocationService).getAllLocationDetails();
    }
}

