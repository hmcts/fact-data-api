package uk.gov.hmcts.reform.fact.data.api.config.properties;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "user", ignoreUnknownFields = false)
@Getter
@Setter
public class UserConfigurationProperties {
    /**
     * Number of days of inactivity after which a user is eligible for retention clean-up.
     */
    @Min(1)
    private long retentionPeriod = 365;
}
