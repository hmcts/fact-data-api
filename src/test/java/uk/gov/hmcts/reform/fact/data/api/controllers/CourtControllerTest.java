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

import uk.gov.hmcts.reform.fact.data.api.entities.AbstractCourtEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDetails;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtDetailsViewService;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_COURT_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "invalid-uuid";
    private static final String COURT_SLUG = "test-court";
    private static final String UNKNOWN_COURT_SLUG = "missing-court";

    private static final int PAGE_NUMBER = 0;
    private static final int PAGE_SIZE = 25;
    private static final Boolean INCLUDE_CLOSED = Boolean.TRUE;
    private static final String REGION_ID = UUID.randomUUID().toString();
    private static final String PARTIAL_COURT_NAME = "Test Court";

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private CourtService courtService;

    @Mock
    private CourtDetailsViewService courtDetailsViewService;

    @InjectMocks
    private CourtController courtController;

    @Test
    void getCourtDetailsByIdReturns200() {
        CourtDetails courtDetails = createCourtDetails();

        when(courtService.getCourtDetailsById(COURT_ID)).thenReturn(courtDetails);

        ResponseEntity<CourtDetails> response = courtController.getCourtDetailsById(COURT_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(courtDetails);
    }

    @Test
    void getCourtDetailsByIdThrowsNotFoundException() {
        when(courtService.getCourtDetailsById(UNKNOWN_COURT_ID)).thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtController.getCourtDetailsById(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getCourtDetailsByIdThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(IllegalArgumentException.class, () ->
            courtController.getCourtDetailsById(INVALID_UUID)
        );
    }

    @Test
    void getCourtDetailsBySlugReturns200() {
        CourtDetails courtDetails = createCourtDetails();

        when(courtService.getCourtDetailsBySlug(COURT_SLUG)).thenReturn(courtDetails);
        when(courtDetailsViewService.prepareDetailsView(courtDetails)).thenReturn(courtDetails);

        ResponseEntity<CourtDetails> response = courtController.getCourtDetailsBySlug(COURT_SLUG);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(courtDetails);
    }

    @Test
    void getCourtDetailsBySlugThrowsNotFoundException() {
        when(courtService.getCourtDetailsBySlug(UNKNOWN_COURT_SLUG))
            .thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            courtController.getCourtDetailsBySlug(UNKNOWN_COURT_SLUG)
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

    private Court createCourt() {
        Court court = new Court();
        populateCourt(court);
        return court;
    }

    private CourtDetails createCourtDetails() {
        CourtDetails court = new CourtDetails();
        populateCourt(court);
        return court;
    }

    private static void populateCourt(final AbstractCourtEntity court) {
        court.setId(COURT_ID);
        court.setName("Test Court Name");
        court.setSlug("test-court");
        court.setOpen(Boolean.TRUE);
        court.setRegionId(UUID.randomUUID());
        court.setIsServiceCentre(Boolean.TRUE);
        court.setOpenOnCath(Boolean.TRUE);
        court.setWarningNotice("Warning notice");
        court.setMrdId("MRD123");
    }
}
