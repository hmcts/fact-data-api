package uk.gov.hmcts.reform.fact.functional.controllers.search;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsLpi;
import uk.gov.hmcts.reform.fact.data.api.os.OsResult;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Search Address Controller")
@DisplayName("Search Address Controller")
public final class SearchAddressControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final String STABLE_ENGLAND_POSTCODE = "SW1A 1AA";
    private static final String STABLE_LPI_POSTCODE = "DH1 3RG";

    @Test
    @DisplayName("GET /search/address/v1/postcode/{postcode} returns valid DPA or LPI address results")
    void shouldReturnAddressesForValidPostcode() throws Exception {
        final OsData osData = TestDataHelper.fetchOsDataForPostcode(http, STABLE_ENGLAND_POSTCODE);

        assertThat(osData.getResults())
            .as("Expected non-empty results for postcode %s", STABLE_ENGLAND_POSTCODE)
            .isNotNull()
            .isNotEmpty()
            .allSatisfy(result -> {
                assertThat(result.getDpa() != null || result.getLpi() != null)
                    .as("Each result should contain DPA or LPI address data")
                    .isTrue();

                if (result.getDpa() != null) {
                    assertThat(result.getDpa().getAddress()).as("DPA ADDRESS should be non-blank").isNotBlank();
                    assertThat(result.getDpa().getPostcode())
                        .as("DPA POSTCODE should contain outward code SW1A")
                        .contains("SW1A");
                } else {
                    assertThat(result.getLpi().getAddress()).as("LPI ADDRESS should be non-blank").isNotBlank();
                    assertThat(result.getLpi().getPostcodeLocator())
                        .as("LPI POSTCODE_LOCATOR should contain outward code SW1A")
                        .contains("SW1A");
                }
            });

        assertThat(osData.getResults())
            .as("Expected at least one DPA result for postcode %s", STABLE_ENGLAND_POSTCODE)
            .anySatisfy(result -> assertThat(result.getDpa()).isNotNull());
    }

    @Test
    @DisplayName("GET /search/address/v1/postcode/{postcode} supports an LPI address postcode")
    void shouldReturnLpiAddressesForKnownLpiPostcode() throws Exception {
        final OsData osData = TestDataHelper.fetchOsDataForPostcode(http, STABLE_LPI_POSTCODE);
        final List<OsLpi> lpiResults = osData.getResults().stream()
            .map(OsResult::getLpi)
            .filter(Objects::nonNull)
            .toList();

        assertThat(lpiResults)
            .as("Expected LPI results for postcode %s", STABLE_LPI_POSTCODE)
            .isNotEmpty()
            .allSatisfy(lpi -> {
                assertThat(lpi.getAddress()).as("LPI ADDRESS should be non-blank").isNotBlank();
                assertThat(lpi.getPostcodeLocator())
                    .as("LPI POSTCODE_LOCATOR should match the requested postcode")
                    .isEqualTo(STABLE_LPI_POSTCODE);
                assertThat(lpi.getUprn()).as("LPI UPRN should be non-blank").isNotBlank();
            });
    }
}
