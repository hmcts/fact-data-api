package uk.gov.hmcts.reform.fact.data.api.config.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoConfigurationPropertiesTest {

    @Test
    void hasDefaultMaxWidthAndAllowsOverride() {
        PhotoConfigurationProperties properties = new PhotoConfigurationProperties();

        assertThat(properties.getMaxWidth()).isEqualTo(1024);

        properties.setMaxWidth(512);

        assertThat(properties.getMaxWidth()).isEqualTo(512);
    }
}

