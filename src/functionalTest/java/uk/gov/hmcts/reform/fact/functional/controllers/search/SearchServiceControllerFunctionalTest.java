package uk.gov.hmcts.reform.fact.functional.controllers.search;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Feature("Search Service Controller")
@DisplayName("Search Service Controller")
public final class SearchServiceControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("GET /search/services/v1 returns all services present in the db")
    void shouldReturnAllServicesSuccessfully() throws Exception {
        final Response response = http.doGet("/search/services/v1");

        assertThat(response.statusCode())
            .as("Expected 200 OK for service search")
            .isEqualTo(OK.value());

        final List<Service> services = mapper.readValue(
            response.getBody().asString(),
            new TypeReference<List<Service>>() {}
        );

        assertThat(services)
            .as("Expected services list to be non-empty")
            .isNotEmpty()
            .as("Expected to see all services to be returned in no specific order")
            .extracting(Service::getName)
            .containsAll(List.of(
                "Money",
                "Probate, divorce or ending civil partnerships",
                "Childcare and parenting",
                "Harm and abuse",
                "Immigration and asylum",
                "Crime",
                "High Court district registries"
            ));

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
            new TypeReference<List<ServiceArea>>() {}
        );

        assertThat(serviceArea)
            .as("Expected to see all service areas to be returned in no specific order")
            .extracting(ServiceArea::getName)
            .containsAll(List.of(
                "Money claims",
                "Probate",
                "Housing",
                "Bankruptcy",
                "Benefits",
                "Claims against employers",
                "Tax",
                "Single Justice Procedure"
            ));

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

    @Test
    @DisplayName("GET /search/services/v1/{serviceName}/service-areas returns 404 for non-existent service")
    void shouldReturn404ForNonExistentService() {
        final String nonExistentService = "Non Existent Service";
        final Response response = http.doGet("/search/services/v1/" + nonExistentService + "/service-areas");

        assertThat(response.statusCode())
            .as("Expected 404 Not Found for non-existent service '%s'", nonExistentService)
            .isEqualTo(NOT_FOUND.value());
    }
}
