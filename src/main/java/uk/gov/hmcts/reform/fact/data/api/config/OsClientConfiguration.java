package uk.gov.hmcts.reform.fact.data.api.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OsClientConfiguration {
    @Value("${os.key}")
    private String key;

    @Bean
    public RequestInterceptor mapitKeyInterceptor() {
        return template -> template.header("key", key);
    }
}
