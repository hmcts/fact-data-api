package uk.gov.hmcts.reform.fact.data.api.config.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditConfigurationPropertiesTest {

    @Test
    void hasDefaultRetentionDaysAndAllowsOverride() {
        AuditConfigurationProperties properties = new AuditConfigurationProperties();

        assertThat(properties.getRetentionDays()).isEqualTo(365);

        properties.setRetentionDays(30);

        assertThat(properties.getRetentionDays()).isEqualTo(30);
    }
}

