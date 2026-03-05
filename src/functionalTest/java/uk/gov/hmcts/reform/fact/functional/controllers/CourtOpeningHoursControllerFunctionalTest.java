package uk.gov.hmcts.reform.fact.functional.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
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

    private UUID courtId;
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

        courtId = UUID.randomUUID();
        court = new Court();
        court.setId(courtId);
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
                .courtId(courtId)
                .openingTimesDetails(openingTimesDetails)
                .build();

        counterServiceOpeningHours =
            CourtCounterServiceOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
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
    @DisplayName("GET /courts/{courtId}/v1/opening-hours retrieves all opening hours for a valid court")
    void shouldGetAllOpeningHoursForCourt() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Opening Hours");
        final UUID openingHourTypeId = TestDataHelper.getOpeningHourTypeId(http, 0);

        openingHours.setId(null);
        openingHours.setCourtId(courtId);
        openingHours.setOpeningHourTypeId(openingHourTypeId);

        final Response putResponse = http.doPut(
            "/courts/" + courtId + "/v1/opening-hours",
            openingHours
        );
        assertThat(putResponse.statusCode()).isEqualTo(OK.value());

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/opening-hours");
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final List<CourtOpeningHours> retrievedHours = mapper.readValue(
            getResponse.asString(),
            new TypeReference<List<CourtOpeningHours>>() {}
        );

        assertThat(retrievedHours).hasSize(1);
        assertThat(retrievedHours.getFirst().getCourtId()).isEqualTo(courtId);
        assertThat(retrievedHours.getFirst().getOpeningHourTypeId()).isEqualTo(openingHourTypeId);
        assertThat(retrievedHours.getFirst().getOpeningTimesDetails()).hasSize(5);
        assertThat(retrievedHours.getFirst().getOpeningTimesDetails().getFirst().getDayOfWeek())
            .isEqualTo(DayOfTheWeek.MONDAY);
        assertThat(retrievedHours.getFirst().getOpeningTimesDetails().getFirst().getOpeningTime())
            .isEqualTo(LocalTime.of(9, 0));
        assertThat(retrievedHours.getFirst().getOpeningTimesDetails().getFirst().getClosingTime())
            .isEqualTo(LocalTime.of(17, 0));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours/counter-service retrieves counter service opening hours")
    void shouldGetCounterServiceOpeningHours() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Counter Service");

        counterServiceOpeningHours.setId(null);
        counterServiceOpeningHours.setCourtId(courtId);

        final Response putResponse = http.doPut(
            "/courts/" + courtId + "/v1/opening-hours/counter-service",
            counterServiceOpeningHours
        );
        assertThat(putResponse.statusCode()).isEqualTo(OK.value());

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/opening-hours/counter-service");
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final CourtCounterServiceOpeningHours retrievedHours = mapper.readValue(
            getResponse.asString(),
            CourtCounterServiceOpeningHours.class
        );

        assertThat(retrievedHours.getOpeningTimesDetails()).hasSize(5);
        assertThat(retrievedHours.getOpeningTimesDetails().getFirst().getOpeningTime())
            .isEqualTo(LocalTime.of(9, 0));
        assertThat(retrievedHours.getOpeningTimesDetails().getFirst().getClosingTime())
            .isEqualTo(LocalTime.of(17, 0));
        assertThat(retrievedHours.getOpeningTimesDetails().stream()
                       .map(OpeningTimesDetail::getDayOfWeek)
                       .toList())
            .containsExactlyInAnyOrder(
                DayOfTheWeek.MONDAY,
                DayOfTheWeek.TUESDAY,
                DayOfTheWeek.WEDNESDAY,
                DayOfTheWeek.THURSDAY,
                DayOfTheWeek.FRIDAY);
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours creates new opening hours")
    void shouldCreateNewOpeningHours() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Create Opening Hours");
        final UUID openingHourTypeId = TestDataHelper.getOpeningHourTypeId(http, 2);

        final ZonedDateTime timestampBeforeCreate = AssertionHelper.getCourtLastUpdatedAt(http, courtId);

        CourtOpeningHours newOpeningHours =
            CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    new OpeningTimesDetail(
                        DayOfTheWeek.MONDAY,
                        LocalTime.of(8, 30),
                        LocalTime.of(18, 0)),
                    new OpeningTimesDetail(
                        DayOfTheWeek.THURSDAY,
                        LocalTime.of(8, 30),
                        LocalTime.of(18, 0))
                ))
                .build();

        final Response putResponse = http.doPut(
            "/courts/" + courtId + "/v1/opening-hours",
            newOpeningHours
        );
        assertThat(putResponse.statusCode()).isEqualTo(OK.value());

        final CourtOpeningHours createdHours = mapper.readValue(
            putResponse.asString(),
            CourtOpeningHours.class
        );

        assertThat(createdHours.getId()).isNotNull();
        assertThat(createdHours.getCourtId()).isEqualTo(courtId);
        assertThat(createdHours.getOpeningHourTypeId()).isEqualTo(openingHourTypeId);
        assertThat(createdHours.getOpeningTimesDetails().stream()
                       .map(OpeningTimesDetail::getDayOfWeek)
                       .toList())
            .containsExactlyInAnyOrder(DayOfTheWeek.MONDAY, DayOfTheWeek.THURSDAY);
        assertThat(createdHours.getOpeningTimesDetails().stream()
                       .allMatch(detail ->
                                     detail.getOpeningTime().equals(LocalTime.of(8, 30))));
        assertThat(createdHours.getOpeningTimesDetails().stream()
                       .allMatch(detail ->
                                     detail.getClosingTime().equals(LocalTime.of(18, 0))));

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/opening-hours/" + createdHours.getId());
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final CourtOpeningHours retrievedHours = mapper.readValue(
            getResponse.asString(),
            CourtOpeningHours.class
        );
        assertThat(retrievedHours.getOpeningTimesDetails().stream()
                       .map(OpeningTimesDetail::getDayOfWeek)
                       .toList())
            .containsExactlyInAnyOrder(DayOfTheWeek.MONDAY, DayOfTheWeek.THURSDAY);

        final ZonedDateTime timestampAfterCreate = AssertionHelper.getCourtLastUpdatedAt(http, courtId);
        assertThat(timestampAfterCreate)
            .as("Court lastUpdatedAt should move forward after opening hours creation for court %s", courtId)
            .isAfter(timestampBeforeCreate);
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/{openingHoursId} replaces existing opening hours")
    void shouldReplaceExistingOpeningHours() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Update Opening Hours");
        final UUID openingHourTypeId = TestDataHelper.getOpeningHourTypeId(http, 3);

        CourtOpeningHours newOpeningHours =
            CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    new OpeningTimesDetail(
                        DayOfTheWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0)),
                    new OpeningTimesDetail(
                        DayOfTheWeek.TUESDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0))
                ))
                .build();

        http.doPut("/courts/" + courtId + "/v1/opening-hours", newOpeningHours);

        final List<CourtOpeningHours> updatedHoursList = List.of(
            CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    new OpeningTimesDetail(
                        DayOfTheWeek.WEDNESDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(16, 0)),
                    new OpeningTimesDetail(
                        DayOfTheWeek.FRIDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(16, 0))
                ))
                .build()
        );

        final Response putResponse = http.doPut(
            "/courts/" + courtId + "/v1/opening-hours",
            updatedHoursList.getFirst()
        );

        assertThat(putResponse.statusCode()).isEqualTo(OK.value());

        final CourtOpeningHours created = mapper.readValue(
            putResponse.asString(),
            new TypeReference<CourtOpeningHours>() {}
        );

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/opening-hours/" + created.getId());
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final CourtOpeningHours retrievedHours = mapper.readValue(
            getResponse.asString(),
            CourtOpeningHours.class
        );

        assertThat(retrievedHours.getOpeningTimesDetails()).hasSize(2);
        assertThat(retrievedHours.getOpeningTimesDetails().stream()
                       .map(OpeningTimesDetail::getDayOfWeek)
                       .toList())
            .containsExactlyInAnyOrder(DayOfTheWeek.WEDNESDAY, DayOfTheWeek.FRIDAY);
        assertThat(retrievedHours.getOpeningTimesDetails().stream()
                       .allMatch(detail ->
                                     detail.getOpeningTime().equals(LocalTime.of(10, 0))));
        assertThat(retrievedHours.getOpeningTimesDetails().stream()
                       .allMatch(detail ->
                                     detail.getClosingTime().equals(LocalTime.of(16, 0))));
        assertThat(retrievedHours.getOpeningTimesDetails().stream()
                       .noneMatch(detail -> detail.getDayOfWeek().equals(DayOfTheWeek.MONDAY)));
        assertThat(retrievedHours.getOpeningTimesDetails().stream()
                       .noneMatch(detail -> detail.getDayOfWeek().equals(DayOfTheWeek.TUESDAY)));
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/counter-service creates counter service opening hours")
    void shouldCreateCounterServiceOpeningHours() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Create Counter Service");

        counterServiceOpeningHours.setId(null); // ensure server generates ID (@GeneratedValue)
        counterServiceOpeningHours.setCourtId(courtId);

        counterServiceOpeningHours.setCounterService(true);
        counterServiceOpeningHours.setAssistWithForms(true);
        counterServiceOpeningHours.setAssistWithDocuments(true);
        counterServiceOpeningHours.setAssistWithSupport(true);

        counterServiceOpeningHours.setOpeningTimesDetails(List.of(
            new OpeningTimesDetail(
                DayOfTheWeek.MONDAY,
                LocalTime.of(10, 0),
                LocalTime.of(15, 0)
            ),
            new OpeningTimesDetail(
                DayOfTheWeek.WEDNESDAY,
                LocalTime.of(10, 0),
                LocalTime.of(15, 0)
            )
        ));
        counterServiceOpeningHours.setAppointmentContact("Email: counter@court.gov.uk");
        counterServiceOpeningHours.setAppointmentNeeded(true);

        final Response putResponse = http.doPut(
            "/courts/" + courtId + "/v1/opening-hours/counter-service",
            counterServiceOpeningHours
        );
        assertThat(putResponse.statusCode()).isEqualTo(OK.value());

        final CourtCounterServiceOpeningHours createdHours = mapper.readValue(
            putResponse.asString(),
            new TypeReference<CourtCounterServiceOpeningHours>() {}
        );

        assertThat(createdHours.getId()).isNotNull();
        assertThat(createdHours.getCourtId()).isEqualTo(courtId);
        assertThat(createdHours.getCounterService()).isTrue();
        assertThat(createdHours.getOpeningTimesDetails()).extracting("dayOfWeek")
            .containsExactlyInAnyOrder(DayOfTheWeek.MONDAY, DayOfTheWeek.WEDNESDAY);
        assertThat(createdHours.getOpeningTimesDetails())
            .allMatch(d -> d.getOpeningTime().equals(LocalTime.of(10, 0)));
        assertThat(createdHours.getOpeningTimesDetails())
            .allMatch(d -> d.getClosingTime().equals(LocalTime.of(15, 0)));

        final OpeningTimesDetail mondayDetail = createdHours.getOpeningTimesDetails().stream()
            .filter(d -> d.getDayOfWeek().equals(DayOfTheWeek.MONDAY))
            .findFirst()
            .orElseThrow();
        assertThat(createdHours.getAppointmentNeeded()).isTrue();
        assertThat(createdHours.getAppointmentContact()).isEqualTo("Email: counter@court.gov.uk");

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/opening-hours/counter-service");
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/opening-hours/{openingHoursId} removes opening hours by type")
    void shouldDeleteOpeningHoursById() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Delete Opening Hours");
        final UUID openingHourTypeId = TestDataHelper.getOpeningHourTypeId(http, 4);

        openingHours.setId(null);
        openingHours.setCourtId(courtId);
        openingHours.setOpeningHourTypeId(openingHourTypeId);

        final Response putResp = http.doPut("/courts/" + courtId + "/v1/opening-hours", openingHours);
        assertThat(putResp.statusCode()).isEqualTo(OK.value());
        final CourtOpeningHours putHours = mapper.readValue(
            putResp.asString(),
            CourtOpeningHours.class
        );

        final Response getBeforeDelete = http.doGet("/courts/" + courtId + "/v1/opening-hours/"
                                                        + putHours.getId());
        assertThat(getBeforeDelete.statusCode()).isIn(OK.value(), NO_CONTENT.value());

        final Response deleteResponse = http.doDelete(
            "/courts/" + courtId + "/v1/opening-hours/" + putHours.getId()
        );
        assertThat(deleteResponse.statusCode()).isIn(OK.value(), NO_CONTENT.value());

        final Response getAfterDelete = http.doGet("/courts/" + courtId + "/v1/opening-hours/"
                                                       + putHours.getId());
        assertThat(getAfterDelete.statusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours returns 204 when no opening hours exist")
    void shouldReturn204WhenNoOpeningHours() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court No Opening Hours");

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/opening-hours");
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
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours fails with invalid UUID format")
    void shouldFailPutWithInvalidUuidFormat() throws Exception {
        final String invalidUuid = "invalid-uuid-123";

        openingHours.setCourtId(UUID.randomUUID());
        openingHours.setOpeningHourTypeId(UUID.randomUUID());

        final Response putResponse = http.doPut(
            "/courts/" + invalidUuid + "/v1/opening-hours",
            openingHours
        );
        assertThat(putResponse.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(putResponse.asString()).contains("Invalid UUID");
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/{openingHoursId} fails with null opening hour")
    void shouldFailPutWithNullOpeningHour() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Null Opening Hour");
        final UUID openingHourTypeId = TestDataHelper.getOpeningHourTypeId(http, 6);

        openingHours.setCourtId(courtId);
        openingHours.setOpeningHourTypeId(openingHourTypeId);
        openingHours.setOpeningTimesDetails(List.of(
            new OpeningTimesDetail(DayOfTheWeek.MONDAY, null, LocalTime.of(17, 0))
        ));

        final Response putResponse = http.doPut(
            "/courts/" + courtId + "/v1/opening-hours",
            openingHours
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

        openingHours.setCourtId(courtId);
        openingHours.setOpeningHourTypeId(openingHourTypeId);
        openingHours.setOpeningTimesDetails(List.of(
            new OpeningTimesDetail(
                DayOfTheWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)),
            new OpeningTimesDetail(
                DayOfTheWeek.EVERYDAY,
                LocalTime.of(10, 0),
                LocalTime.of(16, 0))
        ));

        final Response putResponse = http.doPut(
            "/courts/" + courtId + "/v1/opening-hours",
            openingHours
        );
        assertThat(putResponse.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(putResponse.asString()).contains("Requests for EVERYDAY must be the only entry");
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours"
        + "should successfully save when only EVERYDAY provided")
    void shouldSuccessfullySaveWhenOnlyEverydayProvided() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Everyday Only");
        final UUID openingHourTypeId = TestDataHelper.getOpeningHourTypeId(http, 1);

        CourtOpeningHours everydayOpeningHours =
            CourtOpeningHours.builder()
            .courtId(courtId)
            .openingHourTypeId(openingHourTypeId)
            .openingTimesDetails(List.of(
                new OpeningTimesDetail(
                    DayOfTheWeek.EVERYDAY,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0))
            ))
            .build();

        final Response putResponse = http.doPut(
            "/courts/" + courtId + "/v1/opening-hours",
            everydayOpeningHours
        );
        assertThat(putResponse.statusCode()).isEqualTo(OK.value());

        final CourtOpeningHours created = mapper.readValue(
            putResponse.asString(),
            new TypeReference<CourtOpeningHours>() {}
        );

        final Response getResponse = http.doGet(
            "/courts/" + courtId + "/v1/opening-hours/" + created.getId()
        );
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final CourtOpeningHours retrievedHours = mapper.readValue(
            getResponse.asString(),
            new TypeReference<CourtOpeningHours>() {}
        );

        assertThat(retrievedHours.getOpeningTimesDetails()).hasSize(1);
        assertThat(retrievedHours.getOpeningTimesDetails().getFirst().getDayOfWeek())
            .isEqualTo(DayOfTheWeek.EVERYDAY);
        assertThat(retrievedHours.getOpeningTimesDetails().getFirst().getOpeningTime())
            .isEqualTo(LocalTime.of(9, 0));
        assertThat(retrievedHours.getOpeningTimesDetails().getFirst().getClosingTime())
            .isEqualTo(LocalTime.of(17, 0));
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/courts/name-prefix/Test Court");
    }
}
