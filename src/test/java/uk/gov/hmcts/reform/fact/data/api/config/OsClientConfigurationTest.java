package uk.gov.hmcts.reform.fact.data.api.config;

import feign.RequestTemplate;
import feign.Retryer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OsClientConfigurationTest {

    @Test
    void requestInterceptorAddsApiKeyAndOutputProjection() {
        OsClientConfiguration configuration = new OsClientConfiguration();
        ReflectionTestUtils.setField(configuration, "key", "test-api-key");
        RequestTemplate requestTemplate = new RequestTemplate();

        configuration.osRequestInterceptor().apply(requestTemplate);

        assertThat(requestTemplate.queries().get("key")).containsExactly("test-api-key");
        assertThat(requestTemplate.queries().get("output_srs")).containsExactly("WGS84");
    }

    @Test
    void retryerUsesExpectedBackoffSettings() {
        OsClientConfiguration configuration = new OsClientConfiguration();

        Retryer retryer = configuration.retryer();

        assertThat(retryer).isInstanceOf(Retryer.Default.class);
        assertThat(ReflectionTestUtils.getField(retryer, "period")).isEqualTo(200L);
        assertThat(ReflectionTestUtils.getField(retryer, "maxPeriod")).isEqualTo(2000L);
        assertThat(ReflectionTestUtils.getField(retryer, "maxAttempts")).isEqualTo(3);
    }
}

