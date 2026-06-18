package uk.gov.hmcts.reform.fact.functional.controllers;

import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreContactDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AddressType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.functional.helpers.AssertionHelper;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
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
        ServiceCentre serviceCentre = buildServiceCentre("Top Level");

        Response createResponse = http.doPost("/service-centres/v1", serviceCentre);
        AssertionHelper.assertStatus(createResponse, CREATED);

        UUID serviceCentreId = UUID.fromString(createResponse.jsonPath().getString("id"));
        String createdName = createResponse.jsonPath().getString("name");
        assertThat(createResponse.jsonPath().getBoolean("open")).isFalse();

        Response getResponse = http.doGet("/service-centres/" + serviceCentreId + "/v1");
        AssertionHelper.assertStatus(getResponse, OK);
        assertThat(getResponse.jsonPath().getString("name")).isEqualTo(createdName);

        Response nameResponse = http.doGet("/service-centres/name/v1", Map.of("name", createdName));
        AssertionHelper.assertStatus(nameResponse, OK);
        assertThat(UUID.fromString(nameResponse.jsonPath().getString("id"))).isEqualTo(serviceCentreId);

        serviceCentre.setName(TestDataHelper.appendRandomSuffixToCourtName(TEST_PREFIX + " Updated"));
        serviceCentre.setOpen(true);
        serviceCentre.setWarningNotice("Updated warning notice");

        Response updateResponse = http.doPut("/service-centres/" + serviceCentreId + "/v1", serviceCentre);
        AssertionHelper.assertStatus(updateResponse, OK);
        assertThat(updateResponse.jsonPath().getBoolean("open")).isTrue();
        assertThat(updateResponse.jsonPath().getString("warningNotice")).isEqualTo("Updated warning notice");

        Response missingResponse = http.doGet("/service-centres/" + UUID.randomUUID() + "/v1");
        AssertionHelper.assertStatus(missingResponse, NOT_FOUND);
    }

    @Test
    @DisplayName("Service centre address endpoints support CRUD")
    void shouldCreateReadUpdateAndDeleteServiceCentreAddress() {
        UUID serviceCentreId = createServiceCentre("Address");
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

    @Test
    @DisplayName("Service centre contact details endpoints support CRUD")
    void shouldCreateReadUpdateAndDeleteServiceCentreContactDetails() {
        UUID serviceCentreId = createServiceCentre("Contact");
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

    @Test
    @DisplayName("Service centre areas of law endpoints support get and update")
    void shouldSetAndGetServiceCentreAreasOfLaw() {
        UUID serviceCentreId = createServiceCentre("Areas Of Law");
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

    @Test
    @DisplayName("Testing support can create and cleanup sample service centres")
    void shouldCreateAndCleanupSampleServiceCentre() {
        String serviceCentreName = TestDataHelper.appendRandomSuffixToCourtName(TEST_PREFIX + " Support");

        Response createResponse = http.doGet(
            "/testing-support/service-centres",
            Map.of(
                "serviceCentreName", serviceCentreName,
                "seed", 1,
                "open", true,
                "addWarningNotice", true,
                "withContactDetails", true
            )
        );
        AssertionHelper.assertStatus(createResponse, CREATED);
        assertThat(createResponse.jsonPath().getString("name")).isEqualTo(serviceCentreName);

        Response deleteResponse = http.doDelete("/testing-support/service-centres/name-prefix/" + TEST_PREFIX);
        AssertionHelper.assertStatus(deleteResponse, OK);
    }

    private UUID createServiceCentre(String suffix) {
        ServiceCentre serviceCentre = buildServiceCentre(suffix);

        Response createResponse = http.doPost("/service-centres/v1", serviceCentre);
        AssertionHelper.assertStatus(createResponse, CREATED);

        return UUID.fromString(createResponse.jsonPath().getString("id"));
    }

    private ServiceCentre buildServiceCentre(String suffix) {
        ServiceCentre serviceCentre = new ServiceCentre();
        serviceCentre.setName(TestDataHelper.appendRandomSuffixToCourtName(TEST_PREFIX + " " + suffix));
        serviceCentre.setOpen(false);
        serviceCentre.setServiceAreaIds(List.of(UUID.fromString(http.doGet("/types/v1/service-areas")
                                                                 .jsonPath().getString("[0].id"))));
        serviceCentre.setCatchmentType(CatchmentType.REGIONAL);
        return serviceCentre;
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/service-centres/name-prefix/" + TEST_PREFIX);
    }
}
