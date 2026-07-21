package uk.gov.hmcts.reform.fact.data.api.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fact.data-api.photo", ignoreUnknownFields = false)
@Getter
@Setter
public class PhotoConfigurationProperties {
    @Min(256)
    @Max(1024)
    private int maxWidth = 640;
}
