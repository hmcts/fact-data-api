package uk.gov.hmcts.reform.fact.functional.controllers;

import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AddressType;
import uk.gov.hmcts.reform.fact.functional.helpers.AssertionHelper;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@Feature("Service Centre Address Controller")
@DisplayName("Service Centre Address Controller")
public final class ServiceCentreAddressControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final String TEST_PREFIX = "Test Service Centre Address";

    @Test
    @DisplayName("Service centre address endpoints support CRUD")
    void shouldCreateReadUpdateAndDeleteServiceCentreAddress() {
        UUID serviceCentreId = TestDataHelper.createServiceCentre(http, TEST_PREFIX);
        ServiceCentreAddress address = ServiceCentreAddress.builder()
            .serviceCentreId(serviceCentreId)
            .addressLine1("1 Test Street")
            .townCity("London")
            .postcode("SW1A 1AA")
            .addressType(AddressType.VISIT_OR_CONTACT_US)
            .build();

        Response createResponse = http.doPost("/service-centres/" + serviceCentreId + "/v1/address", address);
        AssertionHelper.assertStatus(createResponse, CREATED);
        UUID addressId = UUID.fromString(createResponse.jsonPath().getString("id"));

        AssertionHelper.assertStatus(http.doGet("/service-centres/" + serviceCentreId + "/v1/address"), OK);

        Response getResponse = http.doGet("/service-centres/" + serviceCentreId + "/v1/address/" + addressId);
        AssertionHelper.assertStatus(getResponse, OK);
        assertThat(getResponse.jsonPath().getString("addressLine1")).isEqualTo("1 Test Street");

        address.setAddressLine1("2 Updated Street");
        Response updateResponse = http.doPut(
            "/service-centres/" + serviceCentreId + "/v1/address/" + addressId,
            address
        );
        AssertionHelper.assertStatus(updateResponse, OK);
        assertThat(updateResponse.jsonPath().getString("addressLine1")).isEqualTo("2 Updated Street");

        AssertionHelper.assertStatus(
            http.doDelete("/service-centres/" + serviceCentreId + "/v1/address/" + addressId),
            NO_CONTENT
        );
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/service-centres/name-prefix/" + TEST_PREFIX);
    }
}
