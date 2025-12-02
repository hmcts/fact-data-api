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
    int capacity = 1000;

    /**
     * Refill interval of the rate-limit bucket.
     */
    int intervalSeconds = 60;

    /**
     * Timeout in seconds for consume requests.
     */
    int timeoutSeconds = 3;

    /**
     * Capacity for a single burst of activity.
     */
    int burstCapacity = 50;

    /**
     * Refill interval of the burst capacity.
     */
    int burstIntervalSeconds = 2;

    /**
     * If true, a per-session instance of this bucket will be created.
     */
    boolean perSession = false;
}
