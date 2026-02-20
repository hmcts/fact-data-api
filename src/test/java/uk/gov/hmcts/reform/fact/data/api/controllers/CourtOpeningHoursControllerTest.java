package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.DayOfTheWeek;
import uk.gov.hmcts.reform.fact.data.api.entities.types.OpeningTimesDetail;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtOpeningHoursService;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtOpeningHoursControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_COURT_ID = UUID.randomUUID();
    private static final UUID OPENING_HOURS_TYPE_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_TYPE_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "abcde";

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";
    private static final String COURT_NOT_FOUND_MESSAGE = "Court not found";
    private static final String OPENING_HOURS_NOT_FOUND_MESSAGE = "Opening hours not found";

    @Mock
    private CourtOpeningHoursService courtOpeningHoursService;

    @InjectMocks
    private CourtOpeningHoursController courtOpeningHoursController;

    private Court court;
    private CourtOpeningHours openingHours;
    private CourtCounterServiceOpeningHours counterServiceOpeningHours;
    private UUID openingHourTypeId;
    private OpeningHourType openingHourType;
    private List<OpeningTimesDetail> openingTimesDetails;

    @BeforeEach
    void setup() {
        openingHourTypeId = UUID.randomUUID();
        openingHourType = new OpeningHourType();
        openingHourType.setId(openingHourTypeId);
        openingHourType.setName("name");

        court = new Court();
        court.setId(COURT_ID);
        court.setName("Test Court");

        openingTimesDetails = List.of(
            new OpeningTimesDetail(
                DayOfTheWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
            ),
            new OpeningTimesDetail(
                DayOfTheWeek.TUESDAY,
                LocalTime.of(10, 0),
                LocalTime.of(17, 0)
            ),
            new OpeningTimesDetail(
                DayOfTheWeek.WEDNESDAY,
                LocalTime.of(9, 0),
                LocalTime.of(16, 0)
            ),
            new OpeningTimesDetail(
                DayOfTheWeek.THURSDAY,
                LocalTime.of(10, 0),
                LocalTime.of(16, 0)
            ),
            new OpeningTimesDetail(
                DayOfTheWeek.FRIDAY,
                LocalTime.of(9, 30),
                LocalTime.of(17, 0)
            )
        );

        openingHours =
            CourtOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(COURT_ID)
                .openingTimesDetails(openingTimesDetails)
                .build();

        counterServiceOpeningHours =
            CourtCounterServiceOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(COURT_ID)
                .openingTimesDetails(openingTimesDetails)
                .appointmentContact("Test Contact")
                .assistWithForms(true)
                .counterService(true)
                .assistWithDocuments(true)
                .assistWithSupport(true)
                .appointmentNeeded(false)
                .build();
    }

    @Test
    void getOpeningHoursByCourtIdReturns200() {
        when(courtOpeningHoursService.getOpeningHoursByCourtId(COURT_ID)).thenReturn(List.of(openingHours));

        ResponseEntity<List<CourtOpeningHours>> response
            = courtOpeningHoursController.getOpeningHoursByCourtId(COURT_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(List.of(openingHours));
    }

    @Test
    void getOpeningHoursThrowsNotFound() {
        when(courtOpeningHoursService.getOpeningHoursByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new CourtResourceNotFoundException(OPENING_HOURS_NOT_FOUND_MESSAGE));

        assertThrows(
            CourtResourceNotFoundException.class, () ->
                courtOpeningHoursController.getOpeningHoursByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getOpeningHoursThrowsCourtNotFoundException() {
        when(courtOpeningHoursService.getOpeningHoursByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new NotFoundException(COURT_NOT_FOUND_MESSAGE));

        assertThrows(
            NotFoundException.class, () ->
                courtOpeningHoursController.getOpeningHoursByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getOpeningHoursThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                courtOpeningHoursController.getOpeningHoursByCourtId(INVALID_UUID)
        );
    }

    @Test
    void getOpeningHoursByTypeIdReturns200() {
        when(courtOpeningHoursService
                 .getOpeningHoursByTypeId(COURT_ID, OPENING_HOURS_TYPE_ID)).thenReturn(openingHours);

        ResponseEntity<CourtOpeningHours> response = courtOpeningHoursController
            .getOpeningHoursByTypeId(COURT_ID.toString(), OPENING_HOURS_TYPE_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(openingHours);
    }

    @Test
    void getOpeningHoursByTypeIdThrowsCourtNotFoundException() {
        when(courtOpeningHoursService.getOpeningHoursByTypeId(UNKNOWN_COURT_ID, UNKNOWN_TYPE_ID))
            .thenThrow(new NotFoundException(COURT_NOT_FOUND_MESSAGE));

        assertThrows(
            NotFoundException.class, () ->
                courtOpeningHoursController
                    .getOpeningHoursByTypeId(UNKNOWN_COURT_ID.toString(), UNKNOWN_TYPE_ID.toString())
        );
    }

    @Test
    void getOpeningHoursByTypeIdThrowsNotFound() {
        when(courtOpeningHoursService.getOpeningHoursByTypeId(COURT_ID, UNKNOWN_TYPE_ID))
            .thenThrow(new CourtResourceNotFoundException(OPENING_HOURS_NOT_FOUND_MESSAGE));

        assertThrows(
            CourtResourceNotFoundException.class, () ->
                courtOpeningHoursController
                    .getOpeningHoursByTypeId(COURT_ID.toString(), UNKNOWN_TYPE_ID.toString())
        );
    }

    @Test
    void getOpeningHoursByTypeIdThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                courtOpeningHoursController.getOpeningHoursByTypeId(INVALID_UUID, INVALID_UUID)
        );
    }

    @Test
    void getCounterServiceOpeningHoursByCourtIdReturns200() {
        when(courtOpeningHoursService.getCounterServiceOpeningHoursByCourtId(COURT_ID))
            .thenReturn(counterServiceOpeningHours);

        ResponseEntity<CourtCounterServiceOpeningHours> response
            = courtOpeningHoursController.getCounterServiceOpeningHoursByCourtId(COURT_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(counterServiceOpeningHours);
    }

    @Test
    void getCounterServiceOpeningHoursThrowsNotFound() {
        when(courtOpeningHoursService.getCounterServiceOpeningHoursByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new CourtResourceNotFoundException(OPENING_HOURS_NOT_FOUND_MESSAGE));

        assertThrows(
            CourtResourceNotFoundException.class, () ->
                courtOpeningHoursController.getCounterServiceOpeningHoursByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getCounterServiceOpeningHoursThrowsCourtNotFoundException() {
        when(courtOpeningHoursService.getCounterServiceOpeningHoursByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new NotFoundException(COURT_NOT_FOUND_MESSAGE));

        assertThrows(
            NotFoundException.class, () ->
                courtOpeningHoursController.getCounterServiceOpeningHoursByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getCounterServiceOpeningHoursThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                courtOpeningHoursController.getCounterServiceOpeningHoursByCourtId(INVALID_UUID)
        );
    }

    @Test
    void setOpeningHoursReturns200() {
        when(courtOpeningHoursService
                 .setOpeningHours(COURT_ID, OPENING_HOURS_TYPE_ID, openingHours)).thenReturn(openingHours);

        ResponseEntity<CourtOpeningHours> response = courtOpeningHoursController
            .setOpeningHours(COURT_ID.toString(), OPENING_HOURS_TYPE_ID.toString(), openingHours);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(openingHours);
    }

    @Test
    void setOpeningHoursThrowsIllegalArgumentExceptionForInvalidCourtId() {
        CourtOpeningHours openingHours = CourtOpeningHours.builder().build();
        assertThrows(
            IllegalArgumentException.class, () ->
                courtOpeningHoursController.setOpeningHours(
                    INVALID_UUID,
                    OPENING_HOURS_TYPE_ID.toString(),
                    openingHours
                )
        );
    }

    @Test
    void setOpeningHoursThrowsIllegalArgumentExceptionForInvalidTypeId() {
        CourtOpeningHours openingHours = CourtOpeningHours.builder().build();
        assertThrows(
            IllegalArgumentException.class, () ->
                courtOpeningHoursController.setOpeningHours(COURT_ID.toString(), INVALID_UUID, openingHours)
        );
    }

    @Test
    void setOpeningHoursThrowsCourtNotFoundException() {
        CourtOpeningHours openingHours = CourtOpeningHours.builder().build();
        when(courtOpeningHoursService.setOpeningHours(UNKNOWN_COURT_ID, OPENING_HOURS_TYPE_ID, openingHours))
            .thenThrow(new NotFoundException(COURT_NOT_FOUND_MESSAGE));

        assertThrows(
            NotFoundException.class, () ->
                courtOpeningHoursController.setOpeningHours(
                    UNKNOWN_COURT_ID.toString(),
                    OPENING_HOURS_TYPE_ID.toString(), openingHours
                )
        );
    }

    @Test
    void setCounterServiceOpeningHoursReturns200() {
        when(courtOpeningHoursService.setCounterServiceOpeningHours(COURT_ID, counterServiceOpeningHours))
            .thenReturn(counterServiceOpeningHours);

        ResponseEntity<CourtCounterServiceOpeningHours> response
            = courtOpeningHoursController.setCounterServiceOpeningHours(
            COURT_ID.toString(),
            counterServiceOpeningHours
        );

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(counterServiceOpeningHours);
    }

    @Test
    void setCounterServiceOpeningHoursThrowsNotFoundException() {
        when(courtOpeningHoursService.setCounterServiceOpeningHours(UNKNOWN_COURT_ID, counterServiceOpeningHours))
            .thenThrow(new NotFoundException(COURT_NOT_FOUND_MESSAGE));

        assertThrows(
            NotFoundException.class, () ->
                courtOpeningHoursController.setCounterServiceOpeningHours(
                    UNKNOWN_COURT_ID.toString(),
                    counterServiceOpeningHours
                )
        );
    }

    @Test
    void setCounterServiceOpeningHoursThrowsIllegalArgumentException() {
        assertThrows(
            IllegalArgumentException.class, () ->
                courtOpeningHoursController.setCounterServiceOpeningHours(INVALID_UUID, counterServiceOpeningHours)
        );
    }


    @Test
    void deleteCourtOpeningHoursReturns200() {
        ResponseEntity<Void> response = courtOpeningHoursController.deleteOpeningHours(
            COURT_ID.toString(),
            OPENING_HOURS_TYPE_ID.toString()
        );
        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteCourtOpeningHoursThrowsNotFoundException() {
        doThrow(new NotFoundException(COURT_NOT_FOUND_MESSAGE))
            .when(courtOpeningHoursService).deleteCourtOpeningHours(UNKNOWN_COURT_ID, OPENING_HOURS_TYPE_ID);

        assertThrows(
            NotFoundException.class, () ->
                courtOpeningHoursController.deleteOpeningHours(
                    UNKNOWN_COURT_ID.toString(),
                    OPENING_HOURS_TYPE_ID.toString()
                )
        );
    }

    @Test
    void deleteOpeningHoursByTypeIdThrowsIllegalArgumentException() {
        assertThrows(
            IllegalArgumentException.class, () ->
                courtOpeningHoursController.deleteOpeningHours(INVALID_UUID, OPENING_HOURS_TYPE_ID.toString())
        );
    }
}
