package uk.gov.hmcts.reform.fact.data.api.migration.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration namespace for the migration helper. Pulls in the legacy FaCT base URL so that the
 * client can be pointed at environment-specific deployments via configuration or .env files.
 */
@Component
@Validated
@ConfigurationProperties(prefix = "migration")
public class MigrationProperties {

    @NotBlank(message = "Migration source base URL must be configured")
    private String sourceBaseUrl;

    public String getSourceBaseUrl() {
        return sourceBaseUrl;
    }

    public void setSourceBaseUrl(String sourceBaseUrl) {
        this.sourceBaseUrl = sourceBaseUrl;
    }
}
