package uk.gov.hmcts.reform.fact.data.api.config.properties;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fact.data-api.rate-limit", ignoreUnknownFields = false)
@Getter
@Setter
public class RateLimitConfigurationProperties {

    /**
     * quick flag to allow postgres to be used as a bucket store.
     */
    public boolean usePostgres = false;

    /**
     * default bucket configuration.
     */
    @NestedConfigurationProperty
    RateLimitBucketConfigurationProperties defaultBucket = new RateLimitBucketConfigurationProperties();

    /**
     * custom bucket configurations.
     */
    @NestedConfigurationProperty
    Map<String, RateLimitBucketConfigurationProperties> buckets;
}
