package uk.gov.hmcts.reform.fact.functional.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsResult;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Feature("Search Controller")
@DisplayName("Search Controller")
public final class SearchControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final String regionId = TestDataHelper.getRegionId(http);
    private static final String STABLE_ENGLAND_POSTCODE = "SW1A 1AA";
    private static final String TEST_COURT_PREFIX = "Test Court";

    @Test
    @DisplayName("GET /search/address/v1/postcode/{postcode} returns addresses for valid postcode")
    void shouldReturnAddressesForValidPostcode() throws Exception {
        final Response response = http.doGet("/search/address/v1/postcode/" + STABLE_ENGLAND_POSTCODE);

        assertThat(response.statusCode())
            .as("Expected 200 OK for valid England postcode %s", STABLE_ENGLAND_POSTCODE)
            .isEqualTo(OK.value());

        final OsData osData = mapper.readValue(response.getBody().asString(), OsData.class);

        assertThat(osData.getResults())
            .as("Expected non-empty results for postcode %s", STABLE_ENGLAND_POSTCODE)
            .isNotNull()
            .isNotEmpty();

        assertThat(osData.getResults())
            .as("Each result should contain valid DPA (Delivery Point Address) data")
            .extracting(OsResult::getDpa)
            .allSatisfy(dpa -> {
                assertThat(dpa.getPostcode())
                    .as("DPA POSTCODE should contain outward code SW1A")
                    .contains("SW1A");
            });

        assertThat(osData.getResults())
            .as("At least one result should have a non-blank address and postcode")
            .extracting(OsResult::getDpa)
            .anySatisfy(dpa -> {
                assertThat(dpa.getAddress())
                    .as("DPA ADDRESS should be non-blank")
                    .isNotBlank();
                assertThat(dpa.getPostcode())
                    .as("DPA POSTCODE should be non-blank")
                    .isNotBlank();
            });
    }

    @Test
    @DisplayName("GET /search/courts/v1/name returns courts matching query")
    void shouldReturnCourtsMatchingNameQuery() throws Exception {
        final String courtName = TEST_COURT_PREFIX + " Birmingham";
        final UUID courtId = TestDataHelper.createCourt(http, courtName);

        final Court courtToUpdate = new Court();
        courtToUpdate.setName(courtName);
        courtToUpdate.setRegionId(UUID.fromString(regionId));
        courtToUpdate.setIsServiceCentre(true);
        courtToUpdate.setOpen(true);

        final Response updateResponse = http.doPut("/courts/" + courtId + "/v1", courtToUpdate);

        assertThat(updateResponse.statusCode())
            .as("Expected 200 OK when updating court to open")
            .isEqualTo(OK.value());

        final Response searchResponse = http.doGet("/search/courts/v1/name?q=Birmingham");

        assertThat(searchResponse.statusCode())
            .as("Expected 200 OK for court name search")
            .isEqualTo(OK.value());

        final List<Court> courts = mapper.readValue(
            searchResponse.getBody().asString(),
            new TypeReference<>() {}
        );

        assertThat(courts)
            .as("Expected to find the created court '%s' in search results", courtName)
            .extracting(Court::getName)
            .contains(courtName);
    }

    @Test
    @DisplayName("GET /search/courts/v1/prefix returns courts starting with letter")
    void shouldReturnCourtsStartingWithPrefix() throws Exception {
        final String courtName = TEST_COURT_PREFIX + " Alpha";
        final UUID courtId = TestDataHelper.createCourt(http, courtName);

        final Court courtToUpdate = new Court();
        courtToUpdate.setName(courtName);
        courtToUpdate.setRegionId(UUID.fromString(regionId));
        courtToUpdate.setIsServiceCentre(true);
        courtToUpdate.setOpen(true);

        final Response updateResponse = http.doPut("/courts/" + courtId + "/v1", courtToUpdate);

        assertThat(updateResponse.statusCode())
            .as("Expected 200 OK when updating court to open")
            .isEqualTo(OK.value());

        final Response searchResponse = http.doGet("/search/courts/v1/prefix?prefix=T");

        assertThat(searchResponse.statusCode())
            .as("Expected 200 OK for prefix search")
            .isEqualTo(OK.value());

        final List<Court> courts = mapper.readValue(
            searchResponse.getBody().asString(),
            new TypeReference<>() {}
        );

        assertThat(courts)
            .as("Expected to find courts starting with 'T'")
            .isNotEmpty();

        assertThat(courts)
            .as("All returned courts should start with 'T'")
            .extracting(Court::getName)
            .allMatch(name -> name.toUpperCase().startsWith("T"));

        assertThat(courts)
            .as("Expected to find the created court '%s' in prefix results", courtName)
            .extracting(Court::getName)
            .contains(courtName);
    }

    @Test
    @DisplayName("GET /search/services/v1 returns all services present in the db")
    void shouldReturnAllServicesSuccessfully() throws Exception {
        final Response response = http.doGet("/search/services/v1");

        assertThat(response.statusCode())
            .as("Expected 200 OK for service search")
            .isEqualTo(OK.value());

        final List<Service> services = mapper.readValue(
            response.getBody().asString(),
            new TypeReference<>() {}
        );

        final List<String> expectedServiceNames = List.of(
            "Money",
            "Probate, divorce or ending civil partnerships",
            "Childcare and parenting",
            "Harm and abuse",
            "Immigration and asylum",
            "Crime",
            "High Court district registries"
        );

        assertThat(services)
            .as("Expected services list to be non-empty")
            .isNotEmpty();

        assertThat(services)
            .as("Expected to see all services to be returned in no specific order")
            .extracting(Service::getName)
            .containsAll(expectedServiceNames);

        assertThat(services)
            .as("Expected each service to have id and name populated")
            .allSatisfy(service -> {
                assertThat(service.getId())
                    .as("Service id should be present")
                    .isNotNull();
                assertThat(service.getName())
                    .as("Service name should be present")
                    .isNotBlank();
            });
    }

    @Test
    @DisplayName("GET /search/services/v1/{serviceName}/service-areas by service name present in the db")
    void shouldReturnServiceAreasByServiceNameSuccessfully() throws Exception {
        final String serviceName = "Money";
        final Response response = http.doGet("/search/services/v1/" + serviceName + "/service-areas");

        assertThat(response.statusCode())
            .as("Expected 200 OK for service search by service name %s", serviceName)
            .isEqualTo(OK.value());

        final List<ServiceArea> serviceArea = mapper.readValue(
            response.getBody().asString(),
            new TypeReference<>() {}
        );

        final List<String> expectedServiceNames = List.of(
            "Money claims",
            "Probate",
            "Housing",
            "Bankruptcy",
            "Benefits",
            "Claims against employers",
            "Tax",
            "Single Justice Procedure"
        );

        assertThat(serviceArea)
            .as("Expected to see all service areas to be returned in no specific order")
            .extracting(ServiceArea::getName)
            .containsAll(expectedServiceNames);

        assertThat(serviceArea)
            .as("Expected each service area to have id and name populated")
            .allSatisfy(area -> {
                assertThat(area.getId())
                    .as("Service area id should be present")
                    .isNotNull();
                assertThat(area.getName())
                    .as("Service area name should be present")
                    .isNotBlank();
            });
    }

    /**
     * This test asserts for an empty list because there is currently no API endpoint
     * to associate courts with service areas. The only way to create this link is by
     * directly updating the database manually or through a migration script, which is
     * not recommended for functional tests. Once an endpoint to link courts to service
     * areas is available, this test should be updated to create the association and
     * assert that the court is returned.
     */
    @Test
    @DisplayName("GET /search/service-area/v1/{serviceAreaName} returns empty list when no courts are linked")
    void shouldReturnEmptyListForServiceAreaWithNoCourts() throws JsonProcessingException {
        final String serviceAreaName = "Tax";
        final Response response = http.doGet("/search/service-area/v1/" + serviceAreaName);

        assertThat(response.statusCode())
            .as("Expected 200 OK for valid service area name '%s'", serviceAreaName)
            .isEqualTo(OK.value());

        final List<Court> courts = mapper.readValue(
            response.getBody().asString(),
            new TypeReference<>() {}
        );

        assertThat(courts)
            .as("Expected empty list as no courts are currently linked to service area '%s'", serviceAreaName)
            .isEmpty();
    }

    @Test
    @DisplayName("GET /search/address/v1/postcode/{postcode} returns 400 for Scotland postcode")
    void shouldReturn400ForScotlandPostcode() {
        final String scotlandPostcode = "EH1 1AA";
        final Response response = http.doGet("/search/address/v1/postcode/" + scotlandPostcode);

        assertThat(response.statusCode())
            .as("Expected 400 Bad Request for Scotland postcode '%s'", scotlandPostcode)
            .isEqualTo(BAD_REQUEST.value());
    }

    @Test
    @DisplayName("GET /search/service-area/v1/{serviceAreaName} returns 404 for non-existent service area")
    void shouldReturn404ForNonExistentServiceArea() {
        final String nonExistentServiceArea = "Non Existent Service Area";
        final Response response = http.doGet("/search/service-area/v1/" + nonExistentServiceArea);

        assertThat(response.statusCode())
            .as("Expected 404 Not Found for non-existent service area '%s'", nonExistentServiceArea)
            .isEqualTo(NOT_FOUND.value());
    }

    @Test
    @DisplayName("GET /search/services/v1/{serviceName}/service-areas returns 404 for non-existent service")
    void shouldReturn404ForNonExistentService() {
        final String nonExistentService = "Non Existent Service";
        final Response response = http.doGet("/search/services/v1/" + nonExistentService + "/service-areas");

        assertThat(response.statusCode())
            .as("Expected 404 Not Found for non-existent service '%s'", nonExistentService)
            .isEqualTo(NOT_FOUND.value());
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/courts/name-prefix/" + TEST_COURT_PREFIX);
    }
}
