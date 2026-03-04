package uk.gov.hmcts.reform.fact.data.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "clients.slack")
@Getter
@Setter
public class SlackProperties {
    private String token;
    private String channelId;
}
