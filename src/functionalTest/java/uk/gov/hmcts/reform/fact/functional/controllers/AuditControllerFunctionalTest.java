package uk.gov.hmcts.reform.fact.functional.controllers;

import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AddressType;
import uk.gov.hmcts.reform.fact.functional.helpers.AssertionHelper;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Feature("Audit Controller")
@DisplayName("Audit Controller")
public final class AuditControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final String ACTION_INSERT = "INSERT";

    @Test
    @DisplayName("GET /audits/v1 returns court and address audit records for create/update/delete flow")
    void shouldReturnExpectedAuditActionsForCourtAndAddressFlow() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Audit Flow");
        final String addressCollectionPath = "/courts/" + courtId + "/v1/address";
        final Response createAddressResponse = http.doPost(
            addressCollectionPath,
            CourtAddress.builder()
                .courtId(courtId)
                .addressLine1("1 Audit Street")
                .townCity("Audit City")
                .postcode("SW1A 1AA")
                .addressType(AddressType.VISIT_US)
                .build()
        );
        AssertionHelper.assertStatus(createAddressResponse, CREATED);

        final UUID addressId = UUID.fromString(createAddressResponse.jsonPath().getString("id"));
        final String addressItemPath = addressCollectionPath + "/" + addressId;

        final Response updateAddressResponse = http.doPut(
            addressItemPath,
            CourtAddress.builder()
                .courtId(courtId)
                .addressLine1("2 Audit Street")
                .townCity("Audit City")
                .postcode("SW1A 1AA")
                .addressType(AddressType.WRITE_TO_US)
                .build()
        );
        AssertionHelper.assertStatus(updateAddressResponse, OK);

        final Response deleteAddressResponse = http.doDelete(addressItemPath);
        AssertionHelper.assertStatus(deleteAddressResponse, NO_CONTENT);

        final String auditsPath = String.format(
            "/audits/v1?pageNumber=0&pageSize=%d&fromDate=%s&courtId=%s",
            100,
            LocalDate.now().minusDays(1),
            courtId
        );
        final Response auditsResponse = http.doGet(auditsPath);
        AssertionHelper.assertStatus(auditsResponse, OK);

        final List<String> auditedCourtIds = auditsResponse.jsonPath().getList("content.subjectId", String.class);
        assertThat(auditedCourtIds)
            .as("All audit records should be filtered to the requested court ID")
            .isNotEmpty()
            .allMatch(courtId.toString()::equals);

        final List<String> auditedSubjectTypes = auditsResponse.jsonPath().getList("content.subjectType", String.class);
        assertThat(auditedSubjectTypes)
            .as("All audit records should be court subject records")
            .isNotEmpty()
            .allMatch("COURT"::equals);

        final List<String> courtActionTypes = extractActionTypesForEntity(auditsResponse, "Court");
        assertThat(courtActionTypes)
            .as("Expected at least one Court INSERT audit entry")
            .contains(ACTION_INSERT);

        final List<String> addressActionTypes = extractActionTypesForEntity(auditsResponse, "CourtAddress");
        assertThat(addressActionTypes)
            .as("Expected CourtAddress DELETE/UPDATE/INSERT audit actions in descending order")
            .containsExactly("DELETE", "UPDATE", ACTION_INSERT);
    }

    @Test
    @DisplayName("GET /audits/{auditId}/v1 returns a single audit record for an existing id")
    void shouldReturnSingleAuditRecordById() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Audit By Id");
        final String auditsPath = String.format(
            "/audits/v1?pageNumber=0&pageSize=%d&fromDate=%s&courtId=%s",
            50,
            LocalDate.now().minusDays(1),
            courtId
        );

        final Response auditsResponse = http.doGet(auditsPath);
        AssertionHelper.assertStatus(auditsResponse, OK);

        final String auditId = auditsResponse.jsonPath().getString("content[0].id");
        assertThat(auditId).as("Expected at least one audit id to be present").isNotBlank();

        final Response singleAuditResponse = http.doGet("/audits/" + auditId + "/v1");
        AssertionHelper.assertStatus(singleAuditResponse, OK);

        assertThat(singleAuditResponse.jsonPath().getString("id"))
            .as("Returned audit id should match requested id")
            .isEqualTo(auditId);

        assertThat(singleAuditResponse.jsonPath().getString("court.id"))
            .as("Returned audit should belong to the created court")
            .isEqualTo(courtId.toString());
    }

    @Test
    @DisplayName("GET /audits/{auditId}/v1 returns not found for unknown id")
    void shouldReturnNotFoundForUnknownAuditId() {
        final Response response = http.doGet("/audits/" + UUID.randomUUID() + "/v1");
        AssertionHelper.assertStatus(response, NOT_FOUND);
    }

    private static List<String> extractActionTypesForEntity(final Response response, final String actionEntity) {
        return response.jsonPath().getList(
            "content.findAll { it.actionEntity == '" + actionEntity + "' }.actionType",
            String.class
        );
    }

    @AfterAll
    static void cleanUp() {
        http.doDelete("/testing-support/courts/name-prefix/Test Court");
    }
}
