package uk.gov.hmcts.reform.fact.data.api.clients.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CathClientConfiguration {

    @Bean
    public Retryer cathRetryer() {
        return new Retryer.Default(
            TimeUnit.MILLISECONDS.toMillis(500),
            TimeUnit.SECONDS.toMillis(10),
            5
        );
    }
}
