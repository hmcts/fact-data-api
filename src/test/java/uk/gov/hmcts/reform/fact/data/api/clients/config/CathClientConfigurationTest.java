package uk.gov.hmcts.reform.fact.data.api.clients.config;

import feign.Retryer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CathClientConfigurationTest {

    @Test
    void createsConfiguredRetryer() {
        Retryer retryer = new CathClientConfiguration().cathRetryer();

        assertThat(retryer).isInstanceOf(Retryer.Default.class);
    }
}

