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
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AddressType;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@Feature("Court Address Controller")
@DisplayName("Court Address Controller")
public final class CourtAddressControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("GET /courts/{courtId}/v1/address returns empty list when court has no addresses")
    void shouldReturnEmptyListWhenCourtHasNoAddresses() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court No Addresses");

        final Response response = http.doGet("/courts/" + courtId + "/v1/address");

        assertThat(response.statusCode())
            .as("Expected 200 OK for court %s with no addresses", courtId)
            .isEqualTo(OK.value());

        final List<CourtAddress> addresses = mapper.readValue(
            response.getBody().asString(),
            new TypeReference<List<CourtAddress>>() {}
        );

        assertThat(addresses)
            .as("Expected empty address list for newly created court %s", courtId)
            .isEmpty();
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/address returns addresses for court")
    void shouldReturnAddressesForCourt() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court With Address");

        final CourtAddress address = buildMinimalAddress(
            courtId, "addressLine1", "townCity", "NW10 4DX", AddressType.VISIT_US);

        final Response createResponse = http.doPost("/courts/" + courtId + "/v1/address", address);

        assertThat(createResponse.statusCode())
            .as("Expected 201 CREATED when creating address for court %s", courtId)
            .isEqualTo(CREATED.value());

        final UUID addressId = UUID.fromString(createResponse.jsonPath().getString("id"));

        final Response response = http.doGet("/courts/" + courtId + "/v1/address");

        assertThat(response.statusCode())
            .as("Expected 200 OK when retrieving addresses for court %s", courtId)
            .isEqualTo(OK.value());

        final List<CourtAddress> addresses = mapper.readValue(
            response.getBody().asString(),
            new TypeReference<List<CourtAddress>>() {}
        );

        assertThat(addresses)
            .as("Expected address list to contain the created address %s", addressId)
            .filteredOn(a -> addressId.equals(a.getId()))
            .hasSize(1);
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/address returns 404 when court does not exist")
    void shouldReturn404WhenCourtDoesNotExist() {
        final UUID nonExistentCourtId = UUID.randomUUID();

        final Response response = http.doGet("/courts/" + nonExistentCourtId + "/v1/address");

        assertThat(response.statusCode())
            .as("Expected 404 NOT_FOUND for non-existent court %s", nonExistentCourtId)
            .isEqualTo(NOT_FOUND.value());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/address/{addressId} returns address by ID")
    void shouldReturnAddressById() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Get Address By Id");
        final CourtAddress address = buildMinimalAddress(
            courtId, "addressLine1", "townCity", "M1 1AE", AddressType.WRITE_TO_US);

        final Response createResponse = http.doPost("/courts/" + courtId + "/v1/address", address);

        assertThat(createResponse.statusCode())
            .as("Expected 201 CREATED when creating address for court %s", courtId)
            .isEqualTo(CREATED.value());

        final UUID addressId = UUID.fromString(createResponse.jsonPath().getString("id"));

        final Response response = http.doGet("/courts/" + courtId + "/v1/address/" + addressId);

        assertThat(response.statusCode())
            .as("Expected 200 OK when retrieving address %s for court %s", addressId, courtId)
            .isEqualTo(OK.value());

        final CourtAddress fetchedAddress = mapper.readValue(
            response.getBody().asString(),
            CourtAddress.class
        );

        assertThat(fetchedAddress.getId())
            .as("Address ID should match")
            .isEqualTo(addressId);
        assertThat(fetchedAddress.getCourtId())
            .as("Court ID should match")
            .isEqualTo(courtId);
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/address/{addressId} returns 404 when address does not exist")
    void shouldReturn404WhenAddressDoesNotExist() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Address Not Found");
        final UUID nonExistentAddressId = UUID.randomUUID();

        final Response response = http.doGet("/courts/" + courtId + "/v1/address/" + nonExistentAddressId);

        assertThat(response.statusCode())
            .as("Expected 404 NOT_FOUND for non-existent address %s", nonExistentAddressId)
            .isEqualTo(NOT_FOUND.value());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/address creates address successfully")
    void shouldCreateAddressSuccessfully() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Create Address");

        final CourtAddress address = CourtAddress.builder()
            .courtId(courtId)
            .addressLine1("789 New Street")
            .addressLine2("Suite 100")
            .townCity("Birmingham")
            .county("West Midlands")
            .postcode("B1 1HQ")
            .addressType(AddressType.VISIT_OR_CONTACT_US)
            .epimId("EPIM123")
            .build();

        final Response response = http.doPost("/courts/" + courtId + "/v1/address", address);

        assertThat(response.statusCode())
            .as("Expected 201 CREATED when creating address for court %s", courtId)
            .isEqualTo(CREATED.value());

        final CourtAddress createdAddress = mapper.readValue(
            response.getBody().asString(),
            CourtAddress.class
        );

        assertThat(createdAddress.getId())
            .as("Created address should have a generated ID")
            .isNotNull();
        assertThat(createdAddress.getCourtId())
            .as("Court ID should match")
            .isEqualTo(courtId);
        assertThat(createdAddress.getAddressLine1())
            .as("Address line 1 should match")
            .isEqualTo("789 New Street");
        assertThat(createdAddress.getAddressLine2())
            .as("Address line 2 should match")
            .isEqualTo("Suite 100");
        assertThat(createdAddress.getTownCity())
            .as("Town/city should match")
            .isEqualTo("Birmingham");
        assertThat(createdAddress.getCounty())
            .as("County should match")
            .isEqualTo("West Midlands");
        assertThat(createdAddress.getPostcode())
            .as("Postcode should match")
            .isEqualTo("B1 1HQ");
        assertThat(createdAddress.getAddressType())
            .as("Address type should match")
            .isEqualTo(AddressType.VISIT_OR_CONTACT_US);
        assertThat(createdAddress.getEpimId())
            .as("EPIM ID should match")
            .isEqualTo("EPIM123");
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/address returns 404 when court does not exist")
    void shouldReturn404WhenCreatingAddressForNonExistentCourt() {
        final UUID nonExistentCourtId = UUID.randomUUID();

        final CourtAddress address = buildMinimalAddress(
            nonExistentCourtId, "addressLine1", "townCity", "N1 9GU", AddressType.VISIT_US);

        final Response response = http.doPost("/courts/" + nonExistentCourtId + "/v1/address", address);

        assertThat(response.statusCode())
            .as("Expected 404 NOT_FOUND for non-existent court %s", nonExistentCourtId)
            .isEqualTo(NOT_FOUND.value());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/address/{addressId} updates address successfully")
    void shouldUpdateAddressSuccessfully() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Update Address");
        final CourtAddress originalAddress = buildMinimalAddress(
            courtId, "addressLine1", "townCity", "L1 8JQ", AddressType.VISIT_US);

        final Response createResponse = http.doPost("/courts/" + courtId + "/v1/address", originalAddress);

        assertThat(createResponse.statusCode())
            .as("Expected 201 CREATED when creating address")
            .isEqualTo(CREATED.value());

        final UUID addressId = UUID.fromString(createResponse.jsonPath().getString("id"));

        final CourtAddress updatedAddress = CourtAddress.builder()
            .courtId(courtId)
            .addressLine1("Updated Street")
            .addressLine2("Floor 5")
            .townCity("Updated City")
            .county("Updated County")
            .postcode("L2 2DP")
            .addressType(AddressType.WRITE_TO_US)
            .epimId("UPDATED01")
            .build();

        final Response updateResponse = http.doPut(
            "/courts/" + courtId + "/v1/address/" + addressId,
            updatedAddress
        );

        assertThat(updateResponse.statusCode())
            .as("Expected 200 OK when updating address %s", addressId)
            .isEqualTo(OK.value());

        final CourtAddress fetchedAddress = mapper.readValue(
            updateResponse.getBody().asString(),
            CourtAddress.class
        );

        assertThat(fetchedAddress.getId())
            .as("Address ID should remain the same")
            .isEqualTo(addressId);
        assertThat(fetchedAddress.getAddressLine1())
            .as("Address line 1 should be updated")
            .isEqualTo("Updated Street");
        assertThat(fetchedAddress.getAddressLine2())
            .as("Address line 2 should be updated")
            .isEqualTo("Floor 5");
        assertThat(fetchedAddress.getTownCity())
            .as("Town/city should be updated")
            .isEqualTo("Updated City");
        assertThat(fetchedAddress.getCounty())
            .as("County should be updated")
            .isEqualTo("Updated County");
        assertThat(fetchedAddress.getPostcode())
            .as("Postcode should be updated")
            .isEqualTo("L2 2DP");
        assertThat(fetchedAddress.getAddressType())
            .as("Address type should be updated")
            .isEqualTo(AddressType.WRITE_TO_US);
        assertThat(fetchedAddress.getEpimId())
            .as("EPIM ID should be updated")
            .isEqualTo("UPDATED01");
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/address/{addressId} returns 404 when address does not exist")
    void shouldReturn404WhenUpdatingNonExistentAddress() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Update Not Found");
        final UUID nonExistentAddressId = UUID.randomUUID();

        final CourtAddress address = buildMinimalAddress(
            courtId, "addressLine1", "townCity", "S1 2HE", AddressType.VISIT_US);

        final Response response = http.doPut(
            "/courts/" + courtId + "/v1/address/" + nonExistentAddressId,
            address
        );

        assertThat(response.statusCode())
            .as("Expected 404 NOT_FOUND for non-existent address %s", nonExistentAddressId)
            .isEqualTo(NOT_FOUND.value());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/address/{addressId} deletes address successfully")
    void shouldDeleteAddressSuccessfully() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Delete Address");

        final CourtAddress address = buildMinimalAddress(
            courtId, "addressLine1", "townCity", "E1 6AN", AddressType.VISIT_US);

        final Response createResponse = http.doPost("/courts/" + courtId + "/v1/address", address);

        assertThat(createResponse.statusCode())
            .as("Expected 201 CREATED when creating address")
            .isEqualTo(CREATED.value());

        final UUID addressId = UUID.fromString(createResponse.jsonPath().getString("id"));

        final Response deleteResponse = http.doDelete("/courts/" + courtId + "/v1/address/" + addressId);

        assertThat(deleteResponse.statusCode())
            .as("Expected 204 NO_CONTENT when deleting address %s", addressId)
            .isEqualTo(NO_CONTENT.value());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/address/{addressId} returns 404 when address does not exist")
    void shouldReturn404WhenDeletingNonExistentAddress() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Delete Not Found");
        final UUID nonExistentAddressId = UUID.randomUUID();

        final Response response = http.doDelete("/courts/" + courtId + "/v1/address/" + nonExistentAddressId);

        assertThat(response.statusCode())
            .as("Expected 404 NOT_FOUND for non-existent address %s", nonExistentAddressId)
            .isEqualTo(NOT_FOUND.value());
    }

    /**
     * Builds a minimal court address for test setup.
     *
     * @param courtId the court identifier to associate with the address
     * @param addressLine1 the address line 1 to assign
     * @param townCity the town/city to assign
     * @param postcode the postcode to assign
     * @param addressType the address type to assign
     * @return the constructed {@link CourtAddress}
     */
    private static CourtAddress buildMinimalAddress(
        final UUID courtId,
        final String addressLine1,
        final String townCity,
        final String postcode,
        final AddressType addressType) {
        return CourtAddress.builder()
            .courtId(courtId)
            .addressLine1(addressLine1)
            .townCity(townCity)
            .postcode(postcode)
            .addressType(addressType)
            .build();
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/courts/name-prefix/Test Court");
    }
}
