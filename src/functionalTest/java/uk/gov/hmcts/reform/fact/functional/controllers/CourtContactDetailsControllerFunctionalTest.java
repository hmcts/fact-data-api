package uk.gov.hmcts.reform.fact.functional.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtContactDetails;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@Feature("Court Contact Details Controller")
@DisplayName("Court Contact Details Controller")
public final class CourtContactDetailsControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("POST /courts/{courtId}/v1/contact-details with all valid fields")
    void shouldCreateContactDetailWithAllValidFields() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Valid Contact Details");
        final UUID contactDescriptionTypeId = UUID.fromString(http.doGet("/types/v1/contact-description-types")
                                                                  .jsonPath().getString("[0].id"));

        final CourtContactDetails contactDetail = new CourtContactDetails();
        contactDetail.setCourtId(courtId);
        contactDetail.setCourtContactDescriptionId(contactDescriptionTypeId);
        contactDetail.setExplanation("General enquiries");
        contactDetail.setExplanationCy("Ymholiadau cyffredinol");
        contactDetail.setEmail("enquiries@test-court.gov.uk");
        contactDetail.setPhoneNumber("01234567890");

        final Response postResponse = http.doPost("/courts/" + courtId + "/v1/contact-details", contactDetail);
        assertThat(postResponse.statusCode()).isEqualTo(CREATED.value());

        final CourtContactDetails createdContact = mapper.readValue(postResponse.asString(), CourtContactDetails.class);
        assertThat(createdContact.getId()).isNotNull();
        assertThat(createdContact.getCourtId()).isEqualTo(courtId);
        assertThat(createdContact.getExplanation()).isEqualTo("General enquiries");
        assertThat(createdContact.getEmail()).isEqualTo("enquiries@test-court.gov.uk");

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/contact-details");
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/contact-details fails with non-existent court")
    void shouldFailToCreateContactDetailForNonExistentCourt() {
        final UUID nonExistentCourtId = UUID.randomUUID();
        final UUID contactDescriptionTypeId = UUID.fromString(http.doGet("/types/v1/contact-description-types")
                                                                  .jsonPath().getString("[1].id"));

        final CourtContactDetails contactDetail = new CourtContactDetails();
        contactDetail.setCourtId(nonExistentCourtId);
        contactDetail.setCourtContactDescriptionId(contactDescriptionTypeId);
        contactDetail.setExplanation("General enquiries");
        contactDetail.setEmail("enquiries@test-court.gov.uk");
        contactDetail.setPhoneNumber("01234567890");

        final Response postResponse = http.doPost("/courts/" + nonExistentCourtId + "/v1/contact-details",
                                                   contactDetail);
        assertThat(postResponse.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(postResponse.jsonPath().getString("message"))
            .contains("Court not found, ID: " + nonExistentCourtId);
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/contact-details fails with invalid courtId UUID")
    void shouldFailPostWithInvalidUuid() {
        final String invalidUuid = "invalid-uuid";
        final UUID contactDescriptionTypeId = UUID.fromString(http.doGet("/types/v1/contact-description-types")
                                                                  .jsonPath().getString("[1].id"));

        final CourtContactDetails contactDetail = new CourtContactDetails();
        contactDetail.setCourtId(UUID.randomUUID());
        contactDetail.setCourtContactDescriptionId(contactDescriptionTypeId);
        contactDetail.setExplanation("General enquiries");
        contactDetail.setEmail("enquiries@test-court.gov.uk");
        contactDetail.setPhoneNumber("01234567890");

        final Response postResponse = http.doPost("/courts/" + invalidUuid + "/v1/contact-details",
                                                  contactDetail);
        assertThat(postResponse.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(postResponse.jsonPath().getString("message"))
            .contains("Invalid UUID supplied: " + invalidUuid);
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/contact-details fails with non-existent contact description type")
    void shouldFailToCreateContactDetailWithNonExistentContactDescriptionType() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Invalid Contact Description Type");
        final UUID nonExistentContactDescriptionTypeId = UUID.randomUUID();

        final CourtContactDetails contactDetail = new CourtContactDetails();
        contactDetail.setCourtId(courtId);
        contactDetail.setCourtContactDescriptionId(nonExistentContactDescriptionTypeId);
        contactDetail.setExplanation("General enquiries");
        contactDetail.setEmail("enquiries@test-court.gov.uk");
        contactDetail.setPhoneNumber("01234567890");

        final Response postResponse = http.doPost("/courts/" + courtId + "/v1/contact-details", contactDetail);
        assertThat(postResponse.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(postResponse.jsonPath().getString("message"))
            .contains("Contact description type not found");
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/contact-details returns existing contact details")
    void shouldRetrieveExistingContactDetails() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Get List of contact details"
            + " for a specific courtId");
        final UUID contactDescriptionTypeId = UUID.fromString(http.doGet("/types/v1/contact-description-types")
                                                                  .jsonPath().getString("[5].id"));

        final CourtContactDetails contactDetail = new CourtContactDetails();
        contactDetail.setCourtId(courtId);
        contactDetail.setCourtContactDescriptionId(contactDescriptionTypeId);
        contactDetail.setExplanation("Test enquiries");
        contactDetail.setEmail("test@test-court.gov.uk");
        contactDetail.setPhoneNumber("01234567890");

        final Response postResponse = http.doPost("/courts/" + courtId + "/v1/contact-details", contactDetail);
        final CourtContactDetails createdContact = mapper.readValue(postResponse.asString(), CourtContactDetails.class);

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/contact-details");
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final CourtContactDetails[] retrievedContacts = mapper.readValue(getResponse.asString(),
                                                                         CourtContactDetails[].class);
        assertThat(retrievedContacts).hasSize(1);
        assertThat(retrievedContacts[0].getId()).isEqualTo(createdContact.getId());
        assertThat(retrievedContacts[0].getCourtId()).isEqualTo(courtId);
        assertThat(retrievedContacts[0].getExplanation()).isEqualTo("Test enquiries");
        assertThat(retrievedContacts[0].getEmail()).isEqualTo("test@test-court.gov.uk");
        assertThat(retrievedContacts[0].getPhoneNumber()).isEqualTo("01234567890");
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/contact-details fails with non-existent court")
    void shouldFailToGetContactDetailsForNonExistentCourt() {
        final UUID nonExistentCourtId = UUID.randomUUID();

        final Response getResponse = http.doGet("/courts/" + nonExistentCourtId + "/v1/contact-details");
        assertThat(getResponse.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(getResponse.jsonPath().getString("message"))
            .contains("Court not found, ID: " + nonExistentCourtId);
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/contact-details fails with invalid UUID")
    void shouldFailGetListWithInvalidUuid() {
        final String invalidUuid = "invalid-uuid";

        final Response getResponse = http.doGet("/courts/" + invalidUuid + "/v1/contact-details");
        assertThat(getResponse.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(getResponse.jsonPath().getString("message"))
            .contains("Invalid UUID supplied: " + invalidUuid);
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/contact-details/{contactId} returns specific contact detail")
    void shouldRetrieveSpecificContactDetail() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Contact Details Get Single");
        final UUID contactDescriptionTypeId = UUID.fromString(http.doGet("/types/v1/contact-description-types")
                                                                  .jsonPath().getString("[7].id"));

        final CourtContactDetails contactDetail = new CourtContactDetails();
        contactDetail.setCourtId(courtId);
        contactDetail.setCourtContactDescriptionId(contactDescriptionTypeId);
        contactDetail.setExplanation("Specific enquiries");
        contactDetail.setEmail("specific@test-court.gov.uk");
        contactDetail.setPhoneNumber("01234567890");

        final Response postResponse = http.doPost("/courts/" + courtId + "/v1/contact-details", contactDetail);
        final CourtContactDetails createdContact = mapper.readValue(postResponse.asString(), CourtContactDetails.class);

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/contact-details/"
                                                    + createdContact.getId());
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final CourtContactDetails retrievedContact = mapper.readValue(getResponse.asString(),
                                                                      CourtContactDetails.class);
        assertThat(retrievedContact.getId()).isEqualTo(createdContact.getId());
        assertThat(retrievedContact.getCourtId()).isEqualTo(courtId);
        assertThat(retrievedContact.getCourtContactDescriptionId()).isEqualTo(contactDescriptionTypeId);
        assertThat(retrievedContact.getExplanation()).isEqualTo("Specific enquiries");
        assertThat(retrievedContact.getEmail()).isEqualTo("specific@test-court.gov.uk");
        assertThat(retrievedContact.getPhoneNumber()).isEqualTo("01234567890");
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/contact-details/{contactId} fails with non-existent contact")
    void shouldFailToGetNonExistentContactDetail() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Contact Details Get Missing");
        final UUID nonExistentContactId = UUID.randomUUID();

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/contact-details/"
                                                    + nonExistentContactId);
        assertThat(getResponse.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(getResponse.jsonPath().getString("message"))
            .contains("Court contact detail not found");
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/contact-details/{contactId} fails with invalid contactId UUID")
    void shouldFailGetSingleWithInvalidUuid() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Contact Details Get Invalid");
        final String invalidUuid = "invalid-uuid";

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/contact-details/" + invalidUuid);
        assertThat(getResponse.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(getResponse.jsonPath().getString("message"))
            .contains("Invalid UUID supplied: " + invalidUuid);
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/contact-details/{contactId} updates contact detail successfully")
    void shouldUpdateContactDetailSuccessfully() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Contact Details Update");
        final UUID contactDescriptionTypeId = UUID.fromString(http.doGet("/types/v1/contact-description-types")
                                                                  .jsonPath().getString("[10].id"));

        final CourtContactDetails contactDetail = new CourtContactDetails();
        contactDetail.setCourtId(courtId);
        contactDetail.setCourtContactDescriptionId(contactDescriptionTypeId);
        contactDetail.setExplanation("Original enquiries");
        contactDetail.setEmail("original@test-court.gov.uk");
        contactDetail.setPhoneNumber("01234567890");

        final Response postResponse = http.doPost("/courts/" + courtId + "/v1/contact-details", contactDetail);
        final CourtContactDetails createdContact = mapper.readValue(postResponse.asString(), CourtContactDetails.class);

        final CourtContactDetails updatedContact = new CourtContactDetails();
        updatedContact.setCourtId(courtId);
        updatedContact.setCourtContactDescriptionId(contactDescriptionTypeId);
        updatedContact.setExplanation("Updated enquiries");
        updatedContact.setEmail("updated@test-court.gov.uk");
        updatedContact.setPhoneNumber("09876543210");

        final Response putResponse = http.doPut("/courts/" + courtId + "/v1/contact-details/"
                                                    + createdContact.getId(), updatedContact);
        assertThat(putResponse.statusCode()).isEqualTo(OK.value());

        final CourtContactDetails modifiedContact = mapper.readValue(putResponse.asString(), CourtContactDetails.class);
        assertThat(modifiedContact.getExplanation()).isEqualTo("Updated enquiries");
        assertThat(modifiedContact.getEmail()).isEqualTo("updated@test-court.gov.uk");

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/contact-details/"
                                                    + createdContact.getId());
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/contact-details/{contactId} fails with non-existent contact")
    void shouldFailToUpdateNonExistentContactDetail() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Contact Details Update Missing");
        final UUID nonExistentContactId = UUID.randomUUID();
        final UUID contactDescriptionTypeId = UUID.fromString(http.doGet("/types/v1/contact-description-types")
                                                                  .jsonPath().getString("[0].id"));

        final CourtContactDetails updatedContact = new CourtContactDetails();
        updatedContact.setCourtId(courtId);
        updatedContact.setCourtContactDescriptionId(contactDescriptionTypeId);
        updatedContact.setExplanation("Updated enquiries");
        updatedContact.setEmail("updated@test-court.gov.uk");
        updatedContact.setPhoneNumber("09876543210");

        final Response putResponse = http.doPut("/courts/" + courtId + "/v1/contact-details/"
                                                    + nonExistentContactId, updatedContact);
        assertThat(putResponse.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(putResponse.jsonPath().getString("message"))
            .contains("Court contact detail not found");
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/contact-details/{contactId} fails with invalid UUID")
    void shouldFailUpdateWithInvalidUuid() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Contact Details Update Invalid");
        final String invalidUuid = "invalid-uuid";
        final UUID contactDescriptionTypeId = UUID.fromString(http.doGet("/types/v1/contact-description-types")
                                                                  .jsonPath().getString("[5].id"));

        final CourtContactDetails updatedContact = new CourtContactDetails();
        updatedContact.setCourtId(courtId);
        updatedContact.setCourtContactDescriptionId(contactDescriptionTypeId);
        updatedContact.setExplanation("Updated enquiries");
        updatedContact.setEmail("updated@test-court.gov.uk");
        updatedContact.setPhoneNumber("09876543210");

        final Response putResponse = http.doPut("/courts/" + courtId + "/v1/contact-details/"
                                                    + invalidUuid, updatedContact);
        assertThat(putResponse.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(putResponse.jsonPath().getString("message"))
            .contains("Invalid UUID supplied: " + invalidUuid);
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/contact-details/{contactId} fails with non-existent "
        + "contactDescriptionTypeId")
    void shouldFailToUpdateContactDetailWithNonExistentContactDescriptionType() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Update Invalid "
            + "Contact Description Type");
        final UUID contactDescriptionTypeId = UUID.fromString(http.doGet("/types/v1/contact-description-types")
                                                                  .jsonPath().getString("[3].id"));

        final CourtContactDetails contactDetail = new CourtContactDetails();
        contactDetail.setCourtId(courtId);
        contactDetail.setCourtContactDescriptionId(contactDescriptionTypeId);
        contactDetail.setExplanation("Original enquiries");
        contactDetail.setEmail("original@test-court.gov.uk");
        contactDetail.setPhoneNumber("01234567890");

        final Response postResponse = http.doPost("/courts/" + courtId + "/v1/contact-details", contactDetail);
        final CourtContactDetails createdContact = mapper.readValue(postResponse.asString(), CourtContactDetails.class);

        final UUID nonExistentContactDescriptionTypeId = UUID.randomUUID();
        final CourtContactDetails updatedContact = new CourtContactDetails();
        updatedContact.setCourtId(courtId);
        updatedContact.setCourtContactDescriptionId(nonExistentContactDescriptionTypeId);
        updatedContact.setExplanation("Updated enquiries");
        updatedContact.setEmail("updated@test-court.gov.uk");
        updatedContact.setPhoneNumber("09876543210");

        final Response putResponse = http.doPut("/courts/" + courtId + "/v1/contact-details/"
                                                    + createdContact.getId(), updatedContact);
        assertThat(putResponse.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(putResponse.jsonPath().getString("message"))
            .contains("Contact description type not found");
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/contact-details/{contactId} deletes contact detail successfully")
    void shouldDeleteContactDetailSuccessfully() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Contact Details Delete");
        final UUID contactDescriptionTypeId = UUID.fromString(http.doGet("/types/v1/contact-description-types")
                                                                  .jsonPath().getString("[6].id"));

        final CourtContactDetails contactDetail = new CourtContactDetails();
        contactDetail.setCourtId(courtId);
        contactDetail.setCourtContactDescriptionId(contactDescriptionTypeId);
        contactDetail.setExplanation("To be deleted");
        contactDetail.setEmail("delete@test-court.gov.uk");
        contactDetail.setPhoneNumber("01234567890");

        final Response postResponse = http.doPost("/courts/" + courtId + "/v1/contact-details", contactDetail);
        final CourtContactDetails createdContact = mapper.readValue(postResponse.asString(), CourtContactDetails.class);

        final Response deleteResponse = http.doDelete("/courts/" + courtId + "/v1/contact-details/"
                                                          + createdContact.getId());
        assertThat(deleteResponse.statusCode()).isEqualTo(NO_CONTENT.value());

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/contact-details/"
                                                    + createdContact.getId());
        assertThat(getResponse.statusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/contact-details/{contactId} fails with non-existent contact")
    void shouldFailToDeleteNonExistentContactDetail() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Contact Details Delete Missing");
        final UUID nonExistentContactId = UUID.randomUUID();

        final Response deleteResponse = http.doDelete("/courts/" + courtId + "/v1/contact-details/"
                                                          + nonExistentContactId);
        assertThat(deleteResponse.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(deleteResponse.jsonPath().getString("message"))
            .contains("Court contact detail not found");
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/contact-details/{contactId} fails with invalid UUID")
    void shouldFailDeleteWithInvalidUuid() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Contact Details Delete Invalid");
        final String invalidUuid = "invalid-uuid";

        final Response deleteResponse = http.doDelete("/courts/" + courtId + "/v1/contact-details/"
                                                          + invalidUuid);
        assertThat(deleteResponse.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(deleteResponse.jsonPath().getString("message"))
            .contains("Invalid UUID supplied: " + invalidUuid);
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/courts/name-prefix/Test Court");
    }
}
