package uk.gov.hmcts.reform.fact.functional.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.types.DayOfTheWeek;
import uk.gov.hmcts.reform.fact.data.api.entities.types.OpeningTimesDetail;
import uk.gov.hmcts.reform.fact.functional.helpers.AssertionHelper;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@Feature("Court Opening Hours Controller")
@DisplayName("Court Opening Hours Controller")
public final class CourtOpeningHoursControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours retrieves all opening hours for a valid court")
    void shouldGetAllOpeningHoursForCourt() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Opening Hours");
        final UUID openingHourTypeId = TestDataHelper.getOpeningHourTypeId(http, 0);

        final CourtOpeningHours mondayHours = CourtOpeningHours.builder()
            .courtId(courtId)
            .openingHourTypeId(openingHourTypeId)
            .openingTimesDetails(List.of(
                new OpeningTimesDetail(
                    DayOfTheWeek.MONDAY,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0)
                )
            ))
            .build();

        final CourtOpeningHours tuesdayHours = CourtOpeningHours.builder()
            .courtId(courtId)
            .openingHourTypeId(openingHourTypeId)
            .openingTimesDetails(List.of(
                new OpeningTimesDetail(
                    DayOfTheWeek.TUESDAY,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0)
                )
            ))
            .build();

        final List<CourtOpeningHours> openingHoursList = List.of(mondayHours, tuesdayHours);

        final Response putResponse = http.doPut(
            "/courts/" + courtId + "/v1/opening-hours/" + openingHourTypeId,
            openingHoursList
        );
        assertThat(putResponse.statusCode()).isEqualTo(OK.value());

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/opening-hours");
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final List<CourtOpeningHours> retrievedHours = mapper.readValue(
            getResponse.asString(),
            new TypeReference<List<CourtOpeningHours>>() {}
        );

        assertThat(retrievedHours).hasSize(2);
        assertThat(retrievedHours.getFirst().getCourtId()).isEqualTo(courtId);
        assertThat(retrievedHours.getFirst().getOpeningHourTypeId()).isEqualTo(openingHourTypeId);
        assertThat(retrievedHours.getFirst().getOpeningTimesDetails()).hasSize(1);
        assertThat(retrievedHours.getFirst().getOpeningTimesDetails().getFirst().getDayOfWeek())
            .isIn(DayOfTheWeek.MONDAY, DayOfTheWeek.TUESDAY);
        assertThat(retrievedHours.getFirst().getOpeningTimesDetails().getFirst().getOpeningTime())
            .isEqualTo(LocalTime.of(9, 0));
        assertThat(retrievedHours.getFirst().getOpeningTimesDetails().getFirst().getClosingTime())
            .isEqualTo(LocalTime.of(17, 0));

        assertThat(retrievedHours.get(1).getCourtId()).isEqualTo(courtId);
        assertThat(retrievedHours.get(1).getOpeningTimesDetails()).hasSize(1);
        assertThat(retrievedHours.get(1).getOpeningTimesDetails().getFirst().getDayOfWeek())
            .isIn(DayOfTheWeek.MONDAY, DayOfTheWeek.TUESDAY);
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours/{openingHourTypeId} retrieves opening hours filtered by type")
    void shouldGetOpeningHoursByType() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Opening Hours By Type");
        final UUID typeId1 = TestDataHelper.getOpeningHourTypeId(http, 0);
        final UUID typeId2 = TestDataHelper.getOpeningHourTypeId(http, 1);

        final CourtOpeningHours type1Hours = CourtOpeningHours.builder()
            .courtId(courtId)
            .openingHourTypeId(typeId1)
            .openingTimesDetails(List.of(
                new OpeningTimesDetail(
                    DayOfTheWeek.MONDAY,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0)
                ),
                new OpeningTimesDetail(
                    DayOfTheWeek.WEDNESDAY,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0)
                )
            ))
            .build();

        final CourtOpeningHours type2Hours = CourtOpeningHours.builder()
            .courtId(courtId)
            .openingHourTypeId(typeId2)
            .openingTimesDetails(List.of(
                new OpeningTimesDetail(
                    DayOfTheWeek.TUESDAY,
                    LocalTime.of(10, 0),
                    LocalTime.of(16, 0)
                ),
                new OpeningTimesDetail(
                    DayOfTheWeek.FRIDAY,
                    LocalTime.of(10, 0),
                    LocalTime.of(16, 0)
                )
            ))
            .build();

        final Response putType1Response = http.doPut("/courts/" + courtId + "/v1/opening-hours/" + typeId1,
                                                     List.of(type1Hours));
        assertThat(putType1Response.statusCode()).isEqualTo(OK.value());

        final Response putType2Response = http.doPut("/courts/" + courtId + "/v1/opening-hours/" + typeId2,
                                                     List.of(type2Hours));
        assertThat(putType2Response.statusCode()).isEqualTo(OK.value());

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/opening-hours/" + typeId1);
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final CourtOpeningHours retrievedHours = mapper.readValue(getResponse.asString(), CourtOpeningHours.class);

        assertThat(retrievedHours.getCourtId()).isEqualTo(courtId);
        assertThat(retrievedHours.getOpeningHourTypeId()).isEqualTo(typeId1);
        assertThat(retrievedHours.getOpeningTimesDetails()).hasSize(2);

        assertThat(retrievedHours.getOpeningTimesDetails().stream()
                       .map(OpeningTimesDetail::getDayOfWeek)
                       .toList())
            .containsExactlyInAnyOrder(DayOfTheWeek.MONDAY, DayOfTheWeek.WEDNESDAY);

        assertThat(retrievedHours.getOpeningTimesDetails())
            .allMatch(d -> d.getOpeningTime().equals(LocalTime.of(9, 0)));

        assertThat(retrievedHours.getOpeningTimesDetails())
            .allMatch(d -> d.getClosingTime().equals(LocalTime.of(17, 0)));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours/counter-service retrieves counter service opening hours")
    void shouldGetCounterServiceOpeningHours() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Counter Service");

        final List<CourtCounterServiceOpeningHours> counterServiceHours = List.of(
            CourtCounterServiceOpeningHours.builder()
                .courtId(courtId)
                .dayOfWeek(DayOfTheWeek.MONDAY)
                .openingHour(LocalTime.of(9, 30))
                .closingHour(LocalTime.of(16, 30))
                .counterService(true)
                .assistWithForms(true)
                .assistWithDocuments(true)
                .assistWithSupport(false)
                .appointmentNeeded(true)
                .appointmentContact("Call 0123456789")
                .build(),
            CourtCounterServiceOpeningHours.builder()
                .courtId(courtId)
                .dayOfWeek(DayOfTheWeek.FRIDAY)
                .openingHour(LocalTime.of(9, 30))
                .closingHour(LocalTime.of(16, 30))
                .counterService(true)
                .assistWithForms(false)
                .assistWithDocuments(true)
                .assistWithSupport(true)
                .appointmentNeeded(false)
                .build()
        );

        final Response putResponse = http.doPut(
            "/courts/" + courtId + "/v1/opening-hours/counter-service",
            counterServiceHours
        );
        assertThat(putResponse.statusCode()).isEqualTo(OK.value());

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/opening-hours/counter-service");
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final List<CourtCounterServiceOpeningHours> retrievedHours = mapper.readValue(
            getResponse.asString(),
            new TypeReference<List<CourtCounterServiceOpeningHours>>() {}
        );

        assertThat(retrievedHours).hasSize(2);
        assertThat(retrievedHours.getFirst().getCourtId()).isEqualTo(courtId);
        assertThat(retrievedHours.getFirst().getCounterService()).isTrue();
        assertThat(retrievedHours.getFirst().getOpeningHour()).isEqualTo(LocalTime.of(9, 30));
        assertThat(retrievedHours.getFirst().getClosingHour()).isEqualTo(LocalTime.of(16, 30));
        assertThat(retrievedHours).extracting(CourtCounterServiceOpeningHours::getDayOfWeek)
            .containsExactlyInAnyOrder(DayOfTheWeek.MONDAY, DayOfTheWeek.FRIDAY);
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/{openingHourTypeId} creates new opening hours")
    void shouldCreateNewOpeningHours() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Create Opening Hours");
        final UUID openingHourTypeId = TestDataHelper.getOpeningHourTypeId(http, 2);

        final ZonedDateTime timestampBeforeCreate = AssertionHelper.getCourtLastUpdatedAt(http, courtId);

        final List<CourtOpeningHours> openingHoursList = List.of(
            CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    new OpeningTimesDetail(
                        DayOfTheWeek.MONDAY,
                        LocalTime.of(8, 30),
                        LocalTime.of(18, 0))
                ))
                .build(),
            CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    new OpeningTimesDetail(
                        DayOfTheWeek.THURSDAY,
                        LocalTime.of(8, 30),
                        LocalTime.of(18, 0))
                ))
                .build()
        );

        final Response putResponse = http.doPut(
            "/courts/" + courtId + "/v1/opening-hours/" + openingHourTypeId,
            openingHoursList
        );
        assertThat(putResponse.statusCode()).isEqualTo(OK.value());

        final List<CourtOpeningHours> createdHours = mapper.readValue(
            putResponse.asString(),
            new TypeReference<List<CourtOpeningHours>>() {}
        );

        assertThat(createdHours).hasSize(2);
        assertThat(createdHours.getFirst().getId()).isNotNull();
        assertThat(createdHours.getFirst().getCourtId()).isEqualTo(courtId);
        assertThat(createdHours.getFirst().getOpeningHourTypeId()).isEqualTo(openingHourTypeId);
        assertThat(createdHours.stream()
                       .flatMap(h -> h.getOpeningTimesDetails().stream())
                       .map(OpeningTimesDetail::getDayOfWeek)
                       .toList())
            .containsExactlyInAnyOrder(DayOfTheWeek.MONDAY, DayOfTheWeek.THURSDAY);
        assertThat(createdHours.stream()
                       .flatMap(h -> h.getOpeningTimesDetails().stream())
                       .allMatch(detail ->
                                     detail.getOpeningTime().equals(LocalTime.of(8, 30))));
        assertThat(createdHours.stream()
                       .flatMap(h -> h.getOpeningTimesDetails().stream())
                       .allMatch(detail ->
                                     detail.getClosingTime().equals(LocalTime.of(18, 0))));

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/opening-hours/" + openingHourTypeId);
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final List<CourtOpeningHours> retrievedHours = mapper.readValue(
            getResponse.asString(),
            new TypeReference<List<CourtOpeningHours>>() {}
        );
        assertThat(retrievedHours).hasSize(2);
        assertThat(retrievedHours.stream()
                       .flatMap(h -> h.getOpeningTimesDetails().stream())
                       .map(OpeningTimesDetail::getDayOfWeek)
                       .toList())
            .containsExactlyInAnyOrder(DayOfTheWeek.MONDAY, DayOfTheWeek.THURSDAY);

        final ZonedDateTime timestampAfterCreate = AssertionHelper.getCourtLastUpdatedAt(http, courtId);
        assertThat(timestampAfterCreate)
            .as("Court lastUpdatedAt should move forward after opening hours creation for court %s", courtId)
            .isAfter(timestampBeforeCreate);
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/{openingHourTypeId} replaces existing opening hours")
    void shouldReplaceExistingOpeningHours() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Update Opening Hours");
        final UUID openingHourTypeId = TestDataHelper.getOpeningHourTypeId(http, 3);

        final List<CourtOpeningHours> initialHours = List.of(
            CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    new OpeningTimesDetail(
                        DayOfTheWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0))
                ))
                .build(),
            CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    new OpeningTimesDetail(
                        DayOfTheWeek.TUESDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0))
                ))
                .build()
        );

        http.doPut("/courts/" + courtId + "/v1/opening-hours/" + openingHourTypeId, initialHours);

        final List<CourtOpeningHours> updatedHours = List.of(
            CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    new OpeningTimesDetail(
                        DayOfTheWeek.WEDNESDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(16, 0))
                ))
                .build(),
            CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    new OpeningTimesDetail(
                        DayOfTheWeek.FRIDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(16, 0))
                ))
                .build()
        );

        final Response putResponse = http.doPut(
            "/courts/" + courtId + "/v1/opening-hours/" + openingHourTypeId,
            updatedHours
        );
        assertThat(putResponse.statusCode()).isEqualTo(OK.value());

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/opening-hours/" + openingHourTypeId);
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final List<CourtOpeningHours> retrievedHours = mapper.readValue(
            getResponse.asString(),
            new TypeReference<List<CourtOpeningHours>>() {}
        );

        assertThat(retrievedHours).hasSize(2);
        assertThat(retrievedHours.stream()
                       .flatMap(h -> h.getOpeningTimesDetails().stream())
                       .map(OpeningTimesDetail::getDayOfWeek)
                       .toList())
            .containsExactlyInAnyOrder(DayOfTheWeek.WEDNESDAY, DayOfTheWeek.FRIDAY);
        assertThat(retrievedHours.stream()
                       .flatMap(h -> h.getOpeningTimesDetails().stream())
                       .allMatch(detail ->
                                     detail.getOpeningTime().equals(LocalTime.of(10, 0))));
        assertThat(retrievedHours.stream()
                       .flatMap(h -> h.getOpeningTimesDetails().stream())
                       .allMatch(detail ->
                                     detail.getClosingTime().equals(LocalTime.of(16, 0))));
        assertThat(retrievedHours.stream()
                       .flatMap(h -> h.getOpeningTimesDetails().stream())
                       .noneMatch(detail -> detail.getDayOfWeek().equals(DayOfTheWeek.MONDAY)));
        assertThat(retrievedHours.stream()
                       .flatMap(h -> h.getOpeningTimesDetails().stream())
                       .noneMatch(detail -> detail.getDayOfWeek().equals(DayOfTheWeek.TUESDAY)));
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/counter-service creates counter service opening hours")
    void shouldCreateCounterServiceOpeningHours() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Create Counter Service");

        final List<CourtCounterServiceOpeningHours> counterServiceHours = List.of(
            CourtCounterServiceOpeningHours.builder()
                .courtId(courtId)
                .dayOfWeek(DayOfTheWeek.MONDAY)
                .openingHour(LocalTime.of(10, 0))
                .closingHour(LocalTime.of(15, 0))
                .counterService(true)
                .assistWithForms(true)
                .assistWithDocuments(false)
                .assistWithSupport(true)
                .appointmentNeeded(true)
                .appointmentContact("Email: counter@court.gov.uk")
                .build(),
            CourtCounterServiceOpeningHours.builder()
                .courtId(courtId)
                .dayOfWeek(DayOfTheWeek.WEDNESDAY)
                .openingHour(LocalTime.of(10, 0))
                .closingHour(LocalTime.of(15, 0))
                .counterService(true)
                .assistWithForms(false)
                .assistWithDocuments(true)
                .assistWithSupport(false)
                .appointmentNeeded(false)
                .build()
        );

        final Response putResponse = http.doPut(
            "/courts/" + courtId + "/v1/opening-hours/counter-service",
            counterServiceHours
        );
        assertThat(putResponse.statusCode()).isEqualTo(OK.value());

        final List<CourtCounterServiceOpeningHours> createdHours = mapper.readValue(
            putResponse.asString(),
            new TypeReference<List<CourtCounterServiceOpeningHours>>() {}
        );

        assertThat(createdHours).hasSize(2);
        assertThat(createdHours.getFirst().getId()).isNotNull();
        assertThat(createdHours.getFirst().getCourtId()).isEqualTo(courtId);
        assertThat(createdHours.getFirst().getCounterService()).isTrue();
        assertThat(createdHours).extracting(CourtCounterServiceOpeningHours::getDayOfWeek)
            .containsExactlyInAnyOrder(DayOfTheWeek.MONDAY, DayOfTheWeek.WEDNESDAY);
        assertThat(createdHours).allMatch(hour -> hour.getOpeningHour()
            .equals(LocalTime.of(10, 0)));
        assertThat(createdHours).allMatch(hour -> hour.getClosingHour()
            .equals(LocalTime.of(15, 0)));

        final CourtCounterServiceOpeningHours mondayHour = createdHours.stream()
            .filter(hour -> hour.getDayOfWeek().equals(DayOfTheWeek.MONDAY))
            .findFirst()
            .orElseThrow();
        assertThat(mondayHour.getAppointmentNeeded()).isTrue();
        assertThat(mondayHour.getAppointmentContact()).isEqualTo("Email: counter@court.gov.uk");

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/opening-hours/counter-service");
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/opening-hours/{openingHourTypeId} removes opening hours by type")
    void shouldDeleteOpeningHoursByType() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Delete Opening Hours");
        final UUID openingHourTypeId = TestDataHelper.getOpeningHourTypeId(http, 4);

        final List<CourtOpeningHours> openingHoursList = List.of(
            CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    new OpeningTimesDetail(
                        DayOfTheWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0))
                ))
                .build(),
            CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    new OpeningTimesDetail(
                        DayOfTheWeek.FRIDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0))
                ))
                .build()
        );

        http.doPut("/courts/" + courtId + "/v1/opening-hours/" + openingHourTypeId, openingHoursList);

        final Response getBeforeDelete = http.doGet("/courts/" + courtId + "/v1/opening-hours/"
                                                        + openingHourTypeId);
        assertThat(getBeforeDelete.statusCode()).isEqualTo(OK.value());

        final Response deleteResponse = http.doDelete(
            "/courts/" + courtId + "/v1/opening-hours/" + openingHourTypeId
        );
        assertThat(deleteResponse.statusCode()).isEqualTo(OK.value());

        final Response getAfterDelete = http.doGet("/courts/" + courtId + "/v1/opening-hours/"
                                                       + openingHourTypeId);
        assertThat(getAfterDelete.statusCode()).isEqualTo(NO_CONTENT.value());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours returns 204 when no opening hours exist")
    void shouldReturn204WhenNoOpeningHours() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court No Opening Hours");

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/opening-hours");
        assertThat(getResponse.statusCode()).isEqualTo(NO_CONTENT.value());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours/{openingHourTypeId} returns 204 when court never had hours")
    void shouldReturn204WhenCourtNeverHadHoursByType() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Never Had Hours");
        final UUID openingHourTypeId = TestDataHelper.getOpeningHourTypeId(http, 5);

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/opening-hours/" + openingHourTypeId);
        assertThat(getResponse.statusCode()).isEqualTo(NO_CONTENT.value());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours returns 404 for non-existent court")
    void shouldReturn404ForNonExistentCourt() throws Exception {
        final UUID nonExistentCourtId = UUID.randomUUID();

        final Response getResponse = http.doGet("/courts/" + nonExistentCourtId + "/v1/opening-hours");
        assertThat(getResponse.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(getResponse.asString()).contains("Court not found");
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/{openingHourTypeId} fails with invalid UUID format")
    void shouldFailPutWithInvalidUuidFormat() throws Exception {
        final String invalidUuid = "invalid-uuid-123";

        final List<CourtOpeningHours> openingHoursList = List.of(
            CourtOpeningHours.builder()
                .courtId(UUID.randomUUID())
                .openingHourTypeId(UUID.randomUUID())
                .openingTimesDetails(List.of(
                    new OpeningTimesDetail(
                        DayOfTheWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0))
                ))
                .build()
        );

        final Response putResponse = http.doPut(
            "/courts/" + invalidUuid + "/v1/opening-hours/" + UUID.randomUUID(),
            openingHoursList
        );
        assertThat(putResponse.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(putResponse.asString()).contains("Invalid UUID");
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/{openingHourTypeId} fails with null opening hour")
    void shouldFailPutWithNullOpeningHour() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Null Opening Hour");
        final UUID openingHourTypeId = TestDataHelper.getOpeningHourTypeId(http, 6);

        final List<CourtOpeningHours> openingHoursList = List.of(
            CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    new OpeningTimesDetail(DayOfTheWeek.MONDAY, null, LocalTime.of(17, 0))
                ))
                .build()
        );

        final Response putResponse = http.doPut(
            "/courts/" + courtId + "/v1/opening-hours/" + openingHourTypeId,
            openingHoursList
        );
        assertThat(putResponse.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(putResponse.asString()).contains("must not be null");
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours fails with invalid UUID format")
    void shouldFailGetWithInvalidUuid() {
        final String invalidUuid = "invalid-uuid";

        final Response getResponse = http.doGet("/courts/" + invalidUuid + "/v1/opening-hours");
        assertThat(getResponse.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(getResponse.asString()).contains("Invalid UUID");
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/{openingHourTypeId} "
        + "should return 400 when EVERYDAY is provided with other days")
    void shouldReturn400WhenEverydayProvidedWithOtherDays() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Everyday Validation");
        final UUID openingHourTypeId = TestDataHelper.getOpeningHourTypeId(http, 7);

        final List<CourtOpeningHours> openingHoursWithEveryday = List.of(
            CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    new OpeningTimesDetail(
                        DayOfTheWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0))
                ))
                .build(),
            CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    new OpeningTimesDetail(
                        DayOfTheWeek.TUESDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0))
                ))
                .build(),
            CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    new OpeningTimesDetail(
                        DayOfTheWeek.EVERYDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(16, 0))
                ))
                .build()
        );

        final Response putResponse = http.doPut(
            "/courts/" + courtId + "/v1/opening-hours/" + openingHourTypeId,
            openingHoursWithEveryday
        );

        assertThat(putResponse.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(putResponse.asString()).contains("Requests for EVERYDAY must be the only entry");
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/{openingHourTypeId} "
        + "should successfully save when only EVERYDAY provided")
    void shouldSuccessfullySaveWhenOnlyEverydayProvided() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Everyday Only");
        final UUID openingHourTypeId = TestDataHelper.getOpeningHourTypeId(http, 8);

        final List<CourtOpeningHours> openingHoursEverydayOnly = List.of(
            CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    new OpeningTimesDetail(
                        DayOfTheWeek.EVERYDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0))
                ))
                .build()
        );

        final Response putResponse = http.doPut(
            "/courts/" + courtId + "/v1/opening-hours/" + openingHourTypeId,
            openingHoursEverydayOnly
        );
        assertThat(putResponse.statusCode()).isEqualTo(OK.value());

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/opening-hours/" + openingHourTypeId);
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final List<CourtOpeningHours> retrievedHours = mapper.readValue(
            getResponse.asString(),
            new TypeReference<List<CourtOpeningHours>>() {}
        );

        assertThat(retrievedHours).hasSize(1);
        assertThat(retrievedHours.getFirst().getOpeningTimesDetails()).hasSize(1);
        assertThat(retrievedHours.getFirst().getOpeningTimesDetails().getFirst().getDayOfWeek())
            .isEqualTo(DayOfTheWeek.EVERYDAY);
        assertThat(retrievedHours.getFirst().getOpeningTimesDetails().getFirst().getOpeningTime())
            .isEqualTo(LocalTime.of(9, 0));
        assertThat(retrievedHours.getFirst().getOpeningTimesDetails().getFirst().getClosingTime())
            .isEqualTo(LocalTime.of(17, 0));
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/courts/name-prefix/Test Court");
    }
}
