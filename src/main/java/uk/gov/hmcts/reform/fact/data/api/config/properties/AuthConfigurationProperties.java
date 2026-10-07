package uk.gov.hmcts.reform.fact.data.api.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "auth", ignoreUnknownFields = false)
@Getter
@Setter
public class AuthConfigurationProperties {

    @NestedConfigurationProperty
    private UserHeaderBypass userHeaderBypass = new UserHeaderBypass();

    @Getter
    @Setter
    public static class UserHeaderBypass {
        /**
         * Comma-separated list of POST endpoints that do not require the user header.
         */
        @NotBlank
        private String postEndpoints = "/courts/v1/link,/user/v1,/csv";

        /**
         * Comma-separated list of DELETE endpoints that do not require the user header.
         */
        @NotBlank
        private String deleteEndpoints = "/user/v1/retention,/audits/v1";

        /**
         * Path prefix for PUT endpoints that do not require the user header.
         */
        @NotBlank
        private String putPrefix = "/courts/v1/link";
    }
}
