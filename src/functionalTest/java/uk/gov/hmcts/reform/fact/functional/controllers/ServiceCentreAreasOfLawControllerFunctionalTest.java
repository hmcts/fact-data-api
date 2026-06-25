package uk.gov.hmcts.reform.fact.functional.controllers;

import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAreasOfLaw;
import uk.gov.hmcts.reform.fact.functional.helpers.AssertionHelper;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@Feature("Service Centre Areas Of Law Controller")
@DisplayName("Service Centre Areas Of Law Controller")
public final class ServiceCentreAreasOfLawControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final String TEST_PREFIX = "Test Service Centre Areas Of Law";

    @Test
    @DisplayName("Service centre areas of law endpoints support get and update")
    void shouldSetAndGetServiceCentreAreasOfLaw() {
        UUID serviceCentreId = TestDataHelper.createServiceCentre(http, TEST_PREFIX);
        UUID areaOfLawId = UUID.fromString(http.doGet("/types/v1/areas-of-law").jsonPath().getString("[0].id"));
        ServiceCentreAreasOfLaw areasOfLaw = ServiceCentreAreasOfLaw.builder()
            .serviceCentreId(serviceCentreId)
            .areasOfLaw(List.of(areaOfLawId))
            .build();

        Response updateResponse = http.doPut(
            "/service-centres/" + serviceCentreId + "/v1/areas-of-law",
            areasOfLaw
        );
        AssertionHelper.assertStatus(updateResponse, CREATED);

        Response getResponse = http.doGet("/service-centres/" + serviceCentreId + "/v1/areas-of-law");
        AssertionHelper.assertStatus(getResponse, OK);
        assertThat(getResponse.jsonPath().getMap("$")).isNotEmpty();
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/service-centres/name-prefix/" + TEST_PREFIX);
    }
}
