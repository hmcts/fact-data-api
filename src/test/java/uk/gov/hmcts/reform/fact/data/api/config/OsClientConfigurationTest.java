package uk.gov.hmcts.reform.fact.data.api.config;

import feign.RequestTemplate;
import feign.Retryer;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.config.properties.OsConfigurationProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.getField;

class OsClientConfigurationTest {

    @Test
    void requestInterceptorAddsApiKeyAndOutputProjection() {
        OsConfigurationProperties osConfigurationProperties = new OsConfigurationProperties();
        osConfigurationProperties.setKey("test-api-key");
        OsClientConfiguration configuration = new OsClientConfiguration(osConfigurationProperties);
        RequestTemplate requestTemplate = new RequestTemplate();

        configuration.osRequestInterceptor().apply(requestTemplate);

        assertThat(requestTemplate.queries().get("key")).containsExactly("test-api-key");
        assertThat(requestTemplate.queries().get("output_srs")).containsExactly("WGS84");
    }

    @Test
    void retryerUsesExpectedBackoffSettings() {
        OsClientConfiguration configuration = new OsClientConfiguration(new OsConfigurationProperties());

        Retryer retryer = configuration.retryer();

        assertThat(retryer).isInstanceOf(Retryer.Default.class);
        assertThat(getField(retryer, "period")).isEqualTo(200L);
        assertThat(getField(retryer, "maxPeriod")).isEqualTo(2000L);
        assertThat(getField(retryer, "maxAttempts")).isEqualTo(3);
    }
}


