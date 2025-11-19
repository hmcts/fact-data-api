package uk.gov.hmcts.reform.fact.data.api.migration.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uk.gov.hmcts.reform.fact.data.api.migration.config.MigrationProperties;
import uk.gov.hmcts.reform.fact.data.api.migration.exception.MigrationClientException;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LegacyExportResponse;

@Component
public class LegacyFactClient {

    private static final String EXPORT_ENDPOINT = "/private-migration/export";

    private final RestClient restClient;

    public LegacyFactClient(
        RestClient.Builder restClientBuilder,
        MigrationProperties properties
    ) {
        this.restClient = restClientBuilder
            .baseUrl(properties.getSourceBaseUrl())
            .build();
    }

    /**
     * Invokes the private migration endpoint on the legacy FaCT service.
     *
     * @return the export payload.
     */
    public LegacyExportResponse fetchExport() {
        try {
            return restClient.get()
                .uri(EXPORT_ENDPOINT)
                .retrieve()
                .body(LegacyExportResponse.class);
        } catch (RestClientException ex) {
            throw new MigrationClientException("Failed to fetch data from legacy FaCT", ex);
        }
    }
}
