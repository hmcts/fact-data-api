package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_COURT_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "invalid-uuid";

    private static final int PAGE_NUMBER = 0;
    private static final int PAGE_SIZE = 25;
    private static final Boolean INCLUDE_CLOSED = Boolean.TRUE;
    private static final String REGION_ID = UUID.randomUUID().toString();
    private static final String PARTIAL_COURT_NAME = "Test Court";

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private CourtService courtService;

    @InjectMocks
    private CourtController courtController;

    @Test
    void getCourtByIdReturns200() {
        Court court = createCourt();

        when(courtService.getCourtById(COURT_ID)).thenReturn(court);

        ResponseEntity<Court> response = courtController.getCourtById(COURT_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(court);
    }

    @Test
    void getCourtByIdThrowsNotFoundException() {
        when(courtService.getCourtById(UNKNOWN_COURT_ID)).thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtController.getCourtById(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getCourtByIdThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(IllegalArgumentException.class, () ->
            courtController.getCourtById(INVALID_UUID)
        );
    }

    @Test
    void getFilteredAndPaginatedCourtsReturns200() {
        Court court = createCourt();
        Page<Court> courtPage = new PageImpl<>(List.of(court));

        when(courtService.getFilteredAndPaginatedCourts(
            any(Pageable.class),
            eq(INCLUDE_CLOSED),
            eq(REGION_ID),
            eq(PARTIAL_COURT_NAME)
        )).thenReturn(courtPage);

        ResponseEntity<Page<Court>> response = courtController.getFilteredAndPaginatedCourts(
            PAGE_NUMBER,
            PAGE_SIZE,
            INCLUDE_CLOSED,
            REGION_ID,
            PARTIAL_COURT_NAME
        );

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(courtPage);
    }

    @Test
    void createCourtReturns201() {
        Court court = createCourt();

        when(courtService.createCourt(court)).thenReturn(court);

        ResponseEntity<Court> response = courtController.createCourt(court);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(court);
    }

    @Test
    void createCourtThrowsNotFoundException() {
        Court court = createCourt();

        when(courtService.createCourt(court)).thenThrow(new NotFoundException("Region not found"));

        assertThrows(NotFoundException.class, () ->
            courtController.createCourt(court)
        );
    }

    @Test
    void updateCourtReturns200() {
        Court court = createCourt();

        when(courtService.updateCourt(COURT_ID, court)).thenReturn(court);

        ResponseEntity<Court> response = courtController.updateCourt(COURT_ID.toString(), court);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(court);
    }

    @Test
    void updateCourtThrowsNotFoundException() {
        Court court = createCourt();

        when(courtService.updateCourt(UNKNOWN_COURT_ID, court)).thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtController.updateCourt(UNKNOWN_COURT_ID.toString(), court)
        );
    }

    @Test
    void updateCourtThrowsIllegalArgumentExceptionForInvalidUUID() {
        Court court = createCourt();

        assertThrows(IllegalArgumentException.class, () ->
            courtController.updateCourt(INVALID_UUID, court)
        );
    }

    @Test
    void linkCaTHCourtsReturns200() {
        List<String> mrdIds = List.of("MRD123", "UNKNOWN");
        Map<String, Object> responseBody = Map.of(
            "matchedLocations", List.of(Map.of("mrdId", "MRD123", "isOpen", true)),
            "unmatchedLocations", List.of("UNKNOWN")
        );

        when(courtService.linkCathCourtsToFact(mrdIds)).thenReturn(responseBody);

        ResponseEntity<Map<String, Object>> response = courtController.linkCaTHCourtsToFaCT(mrdIds);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(responseBody);
    }

    @Test
    void handleCaTHCourtDeletionReturns204() {
        ResponseEntity<Void> response = courtController.handleCaTHCourtDeletion("MRD123");

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.NO_CONTENT);
        verify(courtService).handleCathCourtDeletion("MRD123");
    }

    private Court createCourt() {
        Court court = new Court();
        court.setId(COURT_ID);
        court.setName("Test Court Name");
        court.setSlug("test-court");
        court.setOpen(Boolean.TRUE);
        court.setRegionId(UUID.randomUUID());
        court.setIsServiceCentre(Boolean.TRUE);
        court.setOpenOnCath(Boolean.TRUE);
        court.setWarningNotice("Warning notice");
        court.setMrdId("MRD123");
        return court;
    }
}
