package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtOpeningHoursService;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtOpeningHoursControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_COURT_ID = UUID.randomUUID();
    private static final UUID OPENING_HOURS_TYPE_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "abcde";

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private CourtOpeningHoursService courtOpeningHoursService;

    @InjectMocks
    private CourtOpeningHoursController courtOpeningHoursController;

    @Test
    void getOpeningHoursByCourtIdReturns200() {
        List<CourtOpeningHours> openingHours = List.of(
            CourtOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(COURT_ID)
                .dayOfWeek(1)
                .openingHour(LocalTime.of(9, 0))
                .closingHour(LocalTime.of(17, 0))
                .build()
        );

        when(courtOpeningHoursService.getOpeningHoursByCourtId(COURT_ID)).thenReturn(openingHours);

        var response = courtOpeningHoursController.getOpeningHoursByCourtId(COURT_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(openingHours);
    }

    @Test
    void getOpeningHoursThrowsNotFound() {
        when(courtOpeningHoursService.getOpeningHoursByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new CourtResourceNotFoundException("No opening hours found"));

        assertThrows(
            CourtResourceNotFoundException.class, () ->
                courtOpeningHoursController.getOpeningHoursByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getOpeningHoursThrowsCourtNotFoundException() {
        when(courtOpeningHoursService.getOpeningHoursByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new NotFoundException("Court not found"));

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
    void getCounterServiceOpeningHoursByCourtIdReturns200() {
        CourtCounterServiceOpeningHours openingHours = new CourtCounterServiceOpeningHours();
        openingHours.setCourtId(COURT_ID);
        openingHours.setOpeningHour(LocalTime.ofNanoOfDay(1234567890L));
        openingHours.setClosingHour(LocalTime.ofNanoOfDay(1234567890L));
        openingHours.setDayOfWeek(1);

        when(courtOpeningHoursService.getCounterServiceOpeningHoursByCourtId(COURT_ID)).thenReturn(openingHours);

        var response = courtOpeningHoursController.getCounterServiceOpeningHoursByCourtId(COURT_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(openingHours);
    }

    @Test
    void getCounterServiceOpeningHoursThrowsNotFound() {
        when(courtOpeningHoursService.getCounterServiceOpeningHoursByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new CourtResourceNotFoundException("No opening hours found"));

        assertThrows(
            CourtResourceNotFoundException.class, () ->
                courtOpeningHoursController.getCounterServiceOpeningHoursByCourtId(UNKNOWN_COURT_ID.toString())
        );
    }

    @Test
    void getCounterServiceOpeningHoursThrowsCourtNotFoundException() {
        when(courtOpeningHoursService.getCounterServiceOpeningHoursByCourtId(UNKNOWN_COURT_ID))
            .thenThrow(new NotFoundException("Court not found"));

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
        CourtOpeningHours openingHours = new CourtOpeningHours();
        openingHours.setCourtId(COURT_ID);
        openingHours.setOpeningHourTypeId(OPENING_HOURS_TYPE_ID);
        openingHours.setOpeningHour(LocalTime.ofNanoOfDay(1234567890L));
        openingHours.setClosingHour(LocalTime.ofNanoOfDay(1234567890L));
        openingHours.setDayOfWeek(1);

        when(courtOpeningHoursService
                 .setOpeningHours(COURT_ID, OPENING_HOURS_TYPE_ID, openingHours)).thenReturn(openingHours);

        var response = courtOpeningHoursController
            .setOpeningHours(COURT_ID.toString(), OPENING_HOURS_TYPE_ID.toString(), openingHours);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(openingHours);
    }

}
