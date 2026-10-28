package uk.gov.hmcts.reform.fact.data.api.clients;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import uk.gov.hmcts.reform.fact.data.api.config.OnspdProperties;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.PostcodeReferenceImportException;
import uk.gov.hmcts.reform.fact.data.api.models.OnspdRelease;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OnspdClientTest {

    private MockRestServiceServer server;
    private OnspdClient client;
    private OnspdProperties properties;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        properties = new OnspdProperties();
        properties.setCatalogueUrl("https://catalogue.test/search");
        properties.setAllowedServiceHost("services.test");
        properties.setPageSize(2);
        properties.setMaxAttempts(2);
        properties.setInitialRetryBackoffMillis(0);
        client = new OnspdClient(restTemplate, properties);
    }

    @Test
    void findsLatestHostedTableByReleaseDateAndModification() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://catalogue.test/search")))
            .andRespond(withSuccess("""
                {"results":[
                  {"id":"may","title":"ONS Postcode Directory (May 2026) (Hosted Table)",
                   "url":"https://services.test/may/FeatureServer","modified":2},
                  {"id":"aug-old","title":"ONS Postcode Directory (August 2026) (Hosted Table)",
                   "url":"https://services.test/aug-old/FeatureServer","modified":2},
                  {"id":"aug","title":"ONS Postcode Directory (August 2026) (Hosted Table)",
                   "url":"https://services.test/aug/FeatureServer","modified":3}
                ]}
                """, MediaType.APPLICATION_JSON));

        OnspdRelease release = client.findLatestRelease();

        assertThat(release.itemId()).isEqualTo("aug");
        assertThat(release.releaseDate()).isEqualTo(YearMonth.of(2026, 8));
        assertThat(release.sourceVersion()).isEqualTo("aug:3");
    }

    @Test
    void retrievesFilteredCountAndKeysetPage() {
        OnspdRelease release = release("https://services.test/aug/FeatureServer");
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/0/query")))
            .andExpect(queryParam("returnCountOnly", "true"))
            .andRespond(withSuccess("{\"count\":3}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/0/query")))
            .andExpect(queryParam("outFields", OnspdClient.POSTCODE_FIELDS))
            .andExpect(queryParam("resultRecordCount", "2"))
            .andExpect(request -> assertThat(request.getURI().getQuery()).contains("ObjectId > 2"))
            .andRespond(withSuccess(
                "{\"features\":[{\"attributes\":{\"ObjectId\":3,\"pcds\":\"G2 8GT\"}}]}",
                MediaType.APPLICATION_JSON));

        assertThat(client.getFilteredPostcodeCount(release)).isEqualTo(3);
        List<JsonNode> records = client.getPostcodeRecordsAfter(release, 2);
        assertThat(records).singleElement().satisfies(postcodeRecord ->
            assertThat(postcodeRecord.path("pcds").asText()).isEqualTo("G2 8GT"));
    }

    @Test
    void retriesArcGisErrorsAndReturnsSuccessfulResponse() {
        OnspdRelease release = release("https://services.test/aug/FeatureServer");
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/0/query")))
            .andRespond(withSuccess("{\"error\":{\"code\":500,\"message\":\"temporary\"}}",
                MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/0/query")))
            .andRespond(withSuccess("{\"features\":[]}", MediaType.APPLICATION_JSON));

        assertThat(client.getPostcodeRecordsAfter(release, 0)).isEmpty();
    }

    @Test
    void rejectsUnexpectedHostedTableHost() {
        OnspdRelease unexpectedHostRelease = release("https://unexpected.test/FeatureServer");

        assertThatThrownBy(() -> client.getFilteredPostcodeCount(unexpectedHostRelease))
            .isInstanceOf(PostcodeReferenceImportException.class)
            .hasMessageContaining("unexpected host");
    }

    @Test
    void reportsMissingCatalogueConfigurationWhenSynchronisationIsRequested() {
        properties.setCatalogueUrl(" ");

        assertThatThrownBy(client::findLatestRelease)
            .isInstanceOf(PostcodeReferenceImportException.class)
            .hasMessage("ONSPD catalogue URL is not configured");
    }

    @Test
    void reportsMissingAllowedHostConfigurationWhenPostcodesAreRequested() {
        properties.setAllowedServiceHost(" ");
        OnspdRelease configuredRelease = release("https://services.test/aug/FeatureServer");

        assertThatThrownBy(() -> client.getFilteredPostcodeCount(configuredRelease))
            .isInstanceOf(PostcodeReferenceImportException.class)
            .hasMessage("ONSPD allowed service host is not configured");
    }

    private OnspdRelease release(String serviceUrl) {
        return new OnspdRelease("aug", "August 2026", YearMonth.of(2026, 8), serviceUrl, 3L);
    }
}
