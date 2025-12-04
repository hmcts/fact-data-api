package uk.gov.hmcts.reform.fact.data.api.config;

import feign.RequestInterceptor;
import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.concurrent.TimeUnit.SECONDS;

@Configuration(proxyBeanMethods = false)
public class OsClientConfiguration {
    @Value("${os.key}")
    private String key;

    @Bean
    public RequestInterceptor osRequestInterceptor() {
        return template -> {
            template.query("key", key);
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
