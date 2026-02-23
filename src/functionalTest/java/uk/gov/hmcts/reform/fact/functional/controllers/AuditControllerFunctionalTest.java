package uk.gov.hmcts.reform.fact.functional.controllers;

import io.qameta.allure.Feature;
import io.restassured.response.Response;
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
import static org.springframework.http.HttpStatus.OK;

@Feature("Audit Controller")
@DisplayName("Audit Controller")
public final class AuditControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final int AUDIT_POLL_ATTEMPTS = 12;
    private static final long AUDIT_POLL_SLEEP_MS = 250L;
    private static final int AUDIT_PAGE_SIZE = 100;
    private static final String COURT_ENTITY = "Court";
    private static final String ADDRESS_ENTITY = "CourtAddress";
    private static final String ACTION_INSERT = "INSERT";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_DELETE = "DELETE";

    @Test
    @DisplayName("GET /audits/v1 returns court and address audit records for create/update/delete flow")
    void shouldReturnExpectedAuditActionsForCourtAndAddressFlow() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Audit Flow");
        final Response createAddressResponse = http.doPost(
            addressCollectionPath(courtId),
            createAddressRequest(courtId)
        );
        AssertionHelper.assertStatus(createAddressResponse, CREATED);

        final UUID addressId = UUID.fromString(createAddressResponse.jsonPath().getString("id"));

        final Response updateAddressResponse = http.doPut(
            addressItemPath(courtId, addressId),
            updateAddressRequest(courtId)
        );
        AssertionHelper.assertStatus(updateAddressResponse, OK);

        final Response deleteAddressResponse = http.doDelete(addressItemPath(courtId, addressId));
        AssertionHelper.assertStatus(deleteAddressResponse, NO_CONTENT);

        final Response auditsResponse = getAuditsForCourtWithRetry(courtId);
        AssertionHelper.assertStatus(auditsResponse, OK);

        final List<String> auditedCourtIds = auditsResponse.jsonPath().getList("content.court.id", String.class);
        assertThat(auditedCourtIds)
            .as("All audit records should be filtered to the requested court ID")
            .isNotEmpty()
            .allMatch(courtId.toString()::equals);

        final List<String> courtActionTypes = extractActionTypesForEntity(auditsResponse, COURT_ENTITY);
        assertThat(courtActionTypes)
            .as("Expected at least one Court INSERT audit entry")
            .contains(ACTION_INSERT);

        final List<String> addressActionTypes = extractActionTypesForEntity(auditsResponse, ADDRESS_ENTITY);
        assertThat(addressActionTypes)
            .as("Expected CourtAddress DELETE/UPDATE/INSERT audit actions in descending order")
            .containsExactly(ACTION_DELETE, ACTION_UPDATE, ACTION_INSERT);
    }

    private Response getAuditsForCourtWithRetry(final UUID courtId) {
        Response response = null;
        final String path = String.format(
            "/audits/v1?pageNumber=0&pageSize=%d&fromDate=%s&courtId=%s",
            AUDIT_PAGE_SIZE,
            LocalDate.now().minusDays(1),
            courtId
        );

        for (int i = 0; i < AUDIT_POLL_ATTEMPTS; i++) {
            response = http.doGet(path);

            if (response.statusCode() == OK.value()) {
                final List<String> addressActionTypes = extractActionTypesForEntity(response, ADDRESS_ENTITY);
                if (addressActionTypes != null && addressActionTypes.size() >= 3) {
                    return response;
                }
            }

            try {
                Thread.sleep(AUDIT_POLL_SLEEP_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return response;
    }

    private static CourtAddress createAddressRequest(final UUID courtId) {
        return CourtAddress.builder()
            .courtId(courtId)
            .addressLine1("1 Audit Street")
            .townCity("Audit City")
            .postcode("SW1A 1AA")
            .addressType(AddressType.VISIT_US)
            .build();
    }

    private static CourtAddress updateAddressRequest(final UUID courtId) {
        return CourtAddress.builder()
            .courtId(courtId)
            .addressLine1("2 Audit Street")
            .townCity("Audit City")
            .postcode("SW1A 1AA")
            .addressType(AddressType.WRITE_TO_US)
            .build();
    }

    private static String addressCollectionPath(final UUID courtId) {
        return "/courts/" + courtId + "/v1/address";
    }

    private static String addressItemPath(final UUID courtId, final UUID addressId) {
        return "/courts/" + courtId + "/v1/address/" + addressId;
    }

    private static List<String> extractActionTypesForEntity(final Response response, final String actionEntity) {
        return response.jsonPath().getList(
            "content.findAll { it.actionEntity == '" + actionEntity + "' }.actionType",
            String.class
        );
    }
}
