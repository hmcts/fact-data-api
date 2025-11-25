package uk.gov.hmcts.reform.fact.data.api.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class RateLimitBucketConfigurationProperties {
    /**
     * Capacity of the rate-limit bucket.
     */
    int capacity = 100;

    /**
     * Refill interval of the rate-limit bucket.
     */
    int intervalSeconds = 60;

    /**
     * Timeout in seconds for consume requests.
     */
    int timeoutSeconds = 30;

    /**
     * If true, a per-session instance of this bucket will be created.
     */
    boolean perSession = true;
}
