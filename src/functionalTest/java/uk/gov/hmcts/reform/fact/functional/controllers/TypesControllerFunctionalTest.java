package uk.gov.hmcts.reform.fact.functional.controllers;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.reform.fact.functional.helpers.AssertionHelper.assertJsonArrayResponseContainsValue;

public final class TypesControllerFunctionalTest {

    private static final String TYPES_BASE_PATH = "/types/v1";
    private static final String AREAS_OF_LAW_ENDPOINT = TYPES_BASE_PATH + "/areas-of-law";
    private static final String COURT_TYPES_ENDPOINT = TYPES_BASE_PATH + "/court-types";
    private static final String OPENING_HOURS_TYPES_ENDPOINT = TYPES_BASE_PATH + "/opening-hours-types";
    private static final String CONTACT_DESCRIPTION_TYPES_ENDPOINT = TYPES_BASE_PATH + "/contact-description-types";
    private static final String REGIONS_ENDPOINT = TYPES_BASE_PATH + "/regions";
    private static final String SERVICE_AREAS_ENDPOINT = TYPES_BASE_PATH + "/service-areas";

    private static HttpClient http;

    @BeforeAll
    static void setUp() {
        http = new HttpClient();
    }

    @Test
    @DisplayName("GET /types/v1/areas-of-law returns expected data")
    void shouldReturnAreasOfLawWithExpectedData() {
        assertJsonArrayResponseContainsValue(http, AREAS_OF_LAW_ENDPOINT, OK, "name",
                                             "Divorce");
    }

    @Test
    @DisplayName("GET /types/v1/court-types returns expected data")
    void shouldReturnCourtTypesWithExpectedData() {
        assertJsonArrayResponseContainsValue(http, COURT_TYPES_ENDPOINT, OK, "name",
                                             "Magistrates' Court");
    }

    @Test
    @DisplayName("GET /types/v1/opening-hours-types returns expected data")
    void shouldReturnOpeningHoursTypesWithExpectedData() {
        assertJsonArrayResponseContainsValue(http, OPENING_HOURS_TYPES_ENDPOINT, OK, "name",
                                             "Telephone enquiries answered");
    }

    @Test
    @DisplayName("GET /types/v1/contact-description-types returns expected data")
    void shouldReturnContactDescriptionTypesWithExpectedData() {
        assertJsonArrayResponseContainsValue(http, CONTACT_DESCRIPTION_TYPES_ENDPOINT, OK, "name",
                                             "Accessibility enquiries");
    }

    @Test
    @DisplayName("GET /types/v1/regions returns expected data")
    void shouldReturnRegionsWithExpectedData() {
        assertJsonArrayResponseContainsValue(http, REGIONS_ENDPOINT, OK, "country",
                                             "England");
    }

    @Test
    @DisplayName("GET /types/v1/service-areas returns expected data")
    void shouldReturnServiceAreasWithExpectedData() {
        assertJsonArrayResponseContainsValue(http, SERVICE_AREAS_ENDPOINT, OK, "name",
                                             "Divorce");
    }

    @Test
    @DisplayName("POST request to types endpoints should return 405 Method Not Allowed")
    void shouldFailOnPost() {
        final Response response = http.doPost(AREAS_OF_LAW_ENDPOINT, "");

        assertThat(response.statusCode()).isEqualTo(METHOD_NOT_ALLOWED.value());
    }
}
