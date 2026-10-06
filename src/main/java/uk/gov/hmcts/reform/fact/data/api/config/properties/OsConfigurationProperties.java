package uk.gov.hmcts.reform.fact.data.api.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "os", ignoreUnknownFields = false)
@Getter
@Setter
public class OsConfigurationProperties {
    /**
     * API key used to authenticate requests to the OS Places API.
     */
    @NotBlank
    private String key;

    /**
     * Base URL of the OS Places API.
     */
    @NotBlank
    private String url = "https://api.os.uk";

    @NestedConfigurationProperty
    private Cache cache = new Cache();

    @NestedConfigurationProperty
    private Endpoint endpoint = new Endpoint();

    @Getter
    @Setter
    public static class Cache {
        /**
         * Whether OS lookup results are cached in-memory.
         */
        private boolean enabled = false;

        /**
         * Maximum number of entries held in the OS lookup cache.
         */
        @Min(1)
        private long maximumSize = 1000;

        /**
         * Time, in milliseconds, before a cached OS lookup entry expires.
         */
        @Min(1)
        private long timeToLiveMillis = 3_600_000;
    }

    @Getter
    @Setter
    public static class Endpoint {
        /**
         * OS Places API postcode search path.
         */
        @NotBlank
        private String postcodeSearch = "/search/places/v1/postcode";
    }
}
