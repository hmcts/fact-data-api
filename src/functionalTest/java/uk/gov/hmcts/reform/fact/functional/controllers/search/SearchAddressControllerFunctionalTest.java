package uk.gov.hmcts.reform.fact.functional.controllers.search;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsResult;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Search Address Controller")
@DisplayName("Search Address Controller")
public final class SearchAddressControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final String STABLE_ENGLAND_POSTCODE = "SW1A 1AA";

    @Test
    @DisplayName("GET /search/address/v1/postcode/{postcode} returns addresses for valid postcode")
    void shouldReturnAddressesForValidPostcode() throws Exception {
        final OsData osData = TestDataHelper.fetchOsDataForPostcode(http, STABLE_ENGLAND_POSTCODE);

        assertThat(osData.getResults())
            .as("Expected non-empty results for postcode %s", STABLE_ENGLAND_POSTCODE)
            .isNotNull()
            .isNotEmpty()
            .extracting(OsResult::getDpa)
            .as("Each result should contain valid DPA (Delivery Point Address) data")
            .allSatisfy(dpa -> assertThat(dpa.getPostcode())
                .as("DPA POSTCODE should contain outward code SW1A")
                .contains("SW1A"))
            .as("At least one result should have a non-blank address and postcode")
            .anySatisfy(dpa -> {
                assertThat(dpa.getAddress()).as("DPA ADDRESS should be non-blank").isNotBlank();
                assertThat(dpa.getPostcode()).as("DPA POSTCODE should be non-blank").isNotBlank();
            });
    }
}
