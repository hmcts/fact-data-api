package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAccessibilityOptions;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtAccessibilityOptionsService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtAccessibilityOptionsControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_COURT_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "abcde";

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private CourtAccessibilityOptionsService courtAccessibilityOptionsService;

    @InjectMocks
    private CourtAccessibilityOptionsController courtAccessibilityOptionsController;

    @Test
    void getAccessibilityOptionsServicesReturns200() {
        CourtAccessibilityOptions accessibilityOptions = new CourtAccessibilityOptions();
        accessibilityOptions.setCourtId(COURT_ID);
        accessibilityOptions.setAccessibleEntrance(true);

        when(courtAccessibilityOptionsService.getAccessibilityOptionsByCourtId(COURT_ID))
            .thenReturn(accessibilityOptions);

        var response = courtAccessibilityOptionsController
            .getAccessibilityOptionsServicesByCourtId(COURT_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(accessibilityOptions);
    }

    @Test
    void getAccessibilityOptionsServicesThrowsAccessibilityOptionsNotFound() {
        when(courtAccessibilityOptionsService.getAccessibilityOptionsByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new NotFoundException("No Accessibility Options services found"));

        assertThrows(NotFoundException.class, () ->
            courtAccessibilityOptionsController.getAccessibilityOptionsServicesByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getAccessibilityOptionsServicesThrowsNotFoundException() {
        when(courtAccessibilityOptionsService.getAccessibilityOptionsByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtAccessibilityOptionsController.getAccessibilityOptionsServicesByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getAccessibilityOptionsServicesThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(IllegalArgumentException.class, () ->
            courtAccessibilityOptionsController.getAccessibilityOptionsServicesByCourtId(INVALID_UUID)
        );
    }

    @Test
    void setAccessibilityOptionsServicesReturns201() {
        CourtAccessibilityOptions accessibilityOptions = new CourtAccessibilityOptions();
        accessibilityOptions.setAccessibleEntrance(true);
        accessibilityOptions.setAccessibleParking(false);

        when(courtAccessibilityOptionsService.setAccessibilityOptions(COURT_ID, accessibilityOptions))
            .thenReturn(accessibilityOptions);

        var response = courtAccessibilityOptionsController
            .setAccessibilityOptionsServices(COURT_ID.toString(), accessibilityOptions);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(accessibilityOptions);
    }

    @Test
    void setAccessibilityOptionsServicesThrowsNotFoundExceptionWhenCourtNotFound() {
        CourtAccessibilityOptions accessibilityOptions = new CourtAccessibilityOptions();
        accessibilityOptions.setAccessibleEntrance(true);
        accessibilityOptions.setAccessibleParking(false);

        when(courtAccessibilityOptionsService.setAccessibilityOptions(UNKNOWN_COURT_ID, accessibilityOptions))
            .thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtAccessibilityOptionsController
                .setAccessibilityOptionsServices(UNKNOWN_COURT_ID.toString(), accessibilityOptions)
        );
    }

    @Test
    void setAccessibilityOptionsServicesThrowsIllegalArgumentExceptionForInvalidUUID() {
        CourtAccessibilityOptions accessibilityOptions = new CourtAccessibilityOptions();
        accessibilityOptions.setAccessibleEntrance(true);
        accessibilityOptions.setAccessibleParking(false);

        assertThrows(IllegalArgumentException.class, () ->
            courtAccessibilityOptionsController.setAccessibilityOptionsServices(INVALID_UUID, accessibilityOptions)
        );
    }
}
