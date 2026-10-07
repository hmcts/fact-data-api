package uk.gov.hmcts.reform.fact.data.api.config;

import feign.RequestInterceptor;
import feign.Retryer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.fact.data.api.config.properties.OsConfigurationProperties;

import static java.util.concurrent.TimeUnit.SECONDS;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class OsClientConfiguration {
    private final OsConfigurationProperties osConfigurationProperties;

    @Bean
    public RequestInterceptor osRequestInterceptor() {
        return template -> {
            template.query("key", osConfigurationProperties.getKey());
            template.query("output_srs", "WGS84");
        };
    }

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
            200, SECONDS.toMillis(2), 3
        );
    }
}
