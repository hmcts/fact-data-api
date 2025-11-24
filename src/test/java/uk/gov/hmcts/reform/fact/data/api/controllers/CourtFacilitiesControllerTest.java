package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFacilities;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtFacilitiesService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtFacilitiesControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_COURT_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "abcde";

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private CourtFacilitiesService courtFacilitiesService;

    @InjectMocks
    private CourtFacilitiesController courtFacilitiesController;

    @Test
    void getBuildingFacilitiesReturns200() {
        CourtFacilities facilities = new CourtFacilities();
        facilities.setCourtId(COURT_ID);
        facilities.setParking(true);
        facilities.setBabyChanging(true);

        when(courtFacilitiesService.getFacilitiesByCourtId(COURT_ID)).thenReturn(facilities);

        var response = courtFacilitiesController.getBuildingFacilitiesByCourtId(COURT_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(facilities);
    }

    @Test
    void getBuildingFacilitiesThrowsFacilitiesNotFound() {
        when(courtFacilitiesService.getFacilitiesByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new CourtResourceNotFoundException("No facilities found"));

        assertThrows(CourtResourceNotFoundException.class, () ->
            courtFacilitiesController.getBuildingFacilitiesByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getBuildingFacilitiesThrowsNotFoundException() {
        when(courtFacilitiesService.getFacilitiesByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtFacilitiesController.getBuildingFacilitiesByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getBuildingFacilitiesThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(IllegalArgumentException.class, () ->
            courtFacilitiesController.getBuildingFacilitiesByCourtId(INVALID_UUID)
        );
    }

    @Test
    void setBuildingFacilitiesReturns201() {
        CourtFacilities facilities = new CourtFacilities();
        facilities.setParking(true);
        facilities.setBabyChanging(true);

        when(courtFacilitiesService.setFacilities(COURT_ID, facilities)).thenReturn(facilities);

        var response = courtFacilitiesController.setBuildingFacilities(COURT_ID.toString(), facilities);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(facilities);
    }

    @Test
    void setBuildingFacilitiesThrowsNotFoundExceptionWhenCourtNotFound() {
        CourtFacilities facilities = new CourtFacilities();
        facilities.setParking(true);
        facilities.setBabyChanging(true);

        when(courtFacilitiesService.setFacilities(UNKNOWN_COURT_ID, facilities))
            .thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtFacilitiesController.setBuildingFacilities(UNKNOWN_COURT_ID.toString(), facilities)
        );
    }

    @Test
    void setBuildingFacilitiesThrowsIllegalArgumentExceptionForInvalidUUID() {
        CourtFacilities facilities = new CourtFacilities();
        facilities.setParking(true);

        assertThrows(IllegalArgumentException.class, () ->
            courtFacilitiesController.setBuildingFacilities(INVALID_UUID, facilities)
        );
    }
}
