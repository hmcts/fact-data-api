package uk.gov.hmcts.reform.fact.functional.controllers;

import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.functional.helpers.AssertionHelper;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Feature("Service Centre Controller")
@DisplayName("Service Centre Controller")
public final class ServiceCentreControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final String TEST_PREFIX = "Test Service Centre";

    @Test
    @DisplayName("Service centre top-level endpoints create, read, update and name")
    void shouldCreateReadAndUpdateServiceCentre() {
        ServiceCentre serviceCentre = TestDataHelper.buildServiceCentre(http, TEST_PREFIX + " Top Level");

        Response createResponse = http.doPost("/service-centres/v1", serviceCentre);
        AssertionHelper.assertStatus(createResponse, CREATED);

        UUID serviceCentreId = UUID.fromString(createResponse.jsonPath().getString("id"));
        assertThat(createResponse.jsonPath().getBoolean("open")).isFalse();
        assertThat(createResponse.jsonPath().getString("catchmentType")).isEqualTo(CatchmentType.NATIONAL.name());

        Response getResponse = http.doGet("/service-centres/" + serviceCentreId + "/v1");
        AssertionHelper.assertStatus(getResponse, OK);
        String createdName = createResponse.jsonPath().getString("name");
        assertThat(getResponse.jsonPath().getString("name")).isEqualTo(createdName);

        Response entityResponse = http.doGet("/service-centres/" + serviceCentreId + "/entity/v1");
        AssertionHelper.assertStatus(entityResponse, OK);
        assertThat(entityResponse.jsonPath().getString("name")).isEqualTo(createdName);

        Response nameResponse = http.doGet("/service-centres/name/v1", Map.of("name", createdName));
        AssertionHelper.assertStatus(nameResponse, OK);
        assertThat(UUID.fromString(nameResponse.jsonPath().getString("id"))).isEqualTo(serviceCentreId);

        serviceCentre.setName(TestDataHelper.appendRandomSuffixToCourtName(TEST_PREFIX + " Updated"));
        serviceCentre.setOpen(true);
        serviceCentre.setWarningNotice("Updated warning notice");
        serviceCentre.setCatchmentType(CatchmentType.NATIONAL);

        Response updateResponse = http.doPut("/service-centres/" + serviceCentreId + "/v1", serviceCentre);
        AssertionHelper.assertStatus(updateResponse, OK);
        assertThat(updateResponse.jsonPath().getBoolean("open")).isTrue();
        assertThat(updateResponse.jsonPath().getString("warningNotice")).isEqualTo("Updated warning notice");
        assertThat(updateResponse.jsonPath().getString("catchmentType")).isEqualTo(CatchmentType.NATIONAL.name());

        Response missingResponse = http.doGet("/service-centres/" + UUID.randomUUID() + "/v1");
        AssertionHelper.assertStatus(missingResponse, NOT_FOUND);
    }

    @Test
    @DisplayName("Service centre details endpoint returns child resources")
    void shouldReturnServiceCentreDetailsWithChildResources() {
        Response createResponse = http.doGet(
            "/testing-support/service-centres",
            Map.of(
                "serviceCentreName", TestDataHelper.appendRandomSuffixToCourtName(TEST_PREFIX + " Details"),
                "withContactDetails", "true"
            )
        );
        AssertionHelper.assertStatus(createResponse, CREATED);

        UUID serviceCentreId = UUID.fromString(createResponse.jsonPath().getString("id"));

        Response detailsResponse = http.doGet("/service-centres/" + serviceCentreId + "/v1");
        AssertionHelper.assertStatus(detailsResponse, OK);
        assertThat(detailsResponse.jsonPath().getList("serviceCentreAddresses")).isNotEmpty();
        assertThat(detailsResponse.jsonPath().getList("serviceAreas")).isNotEmpty();
        assertThat(detailsResponse.jsonPath().getList("serviceAreaIds")).isNull();
        assertThat(detailsResponse.jsonPath().getList("serviceCentreContactDetails")).isNotEmpty();
        assertThat(detailsResponse.jsonPath().getList("serviceCentreAreasOfLaw")).isNotEmpty();
        assertThat(detailsResponse.jsonPath().getString(
            "serviceCentreContactDetails[0].serviceCentreContactDescription.id"
        )).isNotBlank();
        assertThat(detailsResponse.jsonPath().getString(
            "serviceCentreContactDetails[0].serviceCentreContactDescriptionId"
        )).isNull();
        assertThat(detailsResponse.jsonPath().getString(
            "serviceCentreAreasOfLaw[0].areasOfLaw[0].id"
        )).isNotBlank();
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/service-centres/name-prefix/" + TEST_PREFIX);
    }
}
