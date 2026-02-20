package uk.gov.hmcts.reform.fact.data.api.config.properties;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fact.data-api.audit", ignoreUnknownFields = false)
@Getter
@Setter
public class AuditConfigurationProperties {
    /**
     * Number of days to retain audit records.
     */
    @Min(7)
    private int retentionDays = 365;
}
