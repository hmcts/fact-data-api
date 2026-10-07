package uk.gov.hmcts.reform.fact.data.api.config.properties;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "court-lock", ignoreUnknownFields = false)
@Getter
@Setter
public class CourtLockConfigurationProperties {
    /**
     * Number of minutes before an unused page lock is considered expired.
     */
    @Min(1)
    private long timeoutMinutes = 60;
}
