package uk.gov.hmcts.reform.fact.functional.controllers;

import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreContactDetails;
import uk.gov.hmcts.reform.fact.functional.helpers.AssertionHelper;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@Feature("Service Centre Contact Details Controller")
@DisplayName("Service Centre Contact Details Controller")
public final class ServiceCentreContactDetailsControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final String TEST_PREFIX = "Test Service Centre Contact";

    @Test
    @DisplayName("Service centre contact details endpoints support CRUD")
    void shouldCreateReadUpdateAndDeleteServiceCentreContactDetails() {
        UUID serviceCentreId = TestDataHelper.createServiceCentre(http, TEST_PREFIX);
        UUID contactDescriptionId = UUID.fromString(
            http.doGet("/types/v1/contact-description-types").jsonPath().getString("[0].id")
        );
        ServiceCentreContactDetails contactDetails = ServiceCentreContactDetails.builder()
            .serviceCentreId(serviceCentreId)
            .serviceCentreContactDescriptionId(contactDescriptionId)
            .explanation("General enquiries")
            .email("servicecentre@example.com")
            .phoneNumber("01234567890")
            .build();

        Response createResponse = http.doPost(
            "/service-centres/" + serviceCentreId + "/v1/contact-details",
            contactDetails
        );
        AssertionHelper.assertStatus(createResponse, CREATED);
        UUID contactId = UUID.fromString(createResponse.jsonPath().getString("id"));

        AssertionHelper.assertStatus(http.doGet("/service-centres/" + serviceCentreId + "/v1/contact-details"), OK);

        Response getResponse = http.doGet(
            "/service-centres/" + serviceCentreId + "/v1/contact-details/" + contactId
        );
        AssertionHelper.assertStatus(getResponse, OK);
        assertThat(getResponse.jsonPath().getString("email")).isEqualTo("servicecentre@example.com");

        contactDetails.setEmail("updated-servicecentre@example.com");
        Response updateResponse = http.doPut(
            "/service-centres/" + serviceCentreId + "/v1/contact-details/" + contactId,
            contactDetails
        );
        AssertionHelper.assertStatus(updateResponse, OK);
        assertThat(updateResponse.jsonPath().getString("email")).isEqualTo("updated-servicecentre@example.com");

        AssertionHelper.assertStatus(
            http.doDelete("/service-centres/" + serviceCentreId + "/v1/contact-details/" + contactId),
            NO_CONTENT
        );
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/service-centres/name-prefix/" + TEST_PREFIX);
    }
}
