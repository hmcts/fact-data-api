package uk.gov.hmcts.reform.fact.data.api.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "onspd")
@Validated
@Getter
@Setter
public class OnspdProperties {

    @NotBlank
    private String catalogueUrl;

    @NotBlank
    private String allowedServiceHost;

    @Min(1)
    private int pageSize = 1000;

    @Min(1)
    private int maxAttempts = 4;

    @PositiveOrZero
    private long initialRetryBackoffMillis = 1000;
}
