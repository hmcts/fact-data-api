package uk.gov.hmcts.reform.fact.data.api.config.properties;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fact.data-api", ignoreUnknownFields = false)
@Getter
@Setter
@Accessors(chain = true)
public class FactDataApiConfigurationProperties {

    /**
     * Rate Limiting Functionality.
     */
    @NestedConfigurationProperty
    RateLimitConfigurationProperties rateLimit;

    /**
     * Redis server configurations.
     */
    @NestedConfigurationProperty
    List<RedisServerConfigurationProperties> redisServers;

}
