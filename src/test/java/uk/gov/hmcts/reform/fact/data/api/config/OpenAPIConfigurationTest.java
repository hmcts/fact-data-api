package uk.gov.hmcts.reform.fact.data.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenAPIConfigurationTest {

    @Test
    void contextLoads() {
        OpenAPI openApi = new OpenAPIConfiguration().openAPI();

        assertEquals("Fact Data API", openApi.getInfo().getTitle());
        assertEquals("v0.0.1", openApi.getInfo().getVersion());
        assertEquals("MIT", openApi.getInfo().getLicense().getName());
        assertEquals("https://opensource.org/licenses/MIT", openApi.getInfo().getLicense().getUrl());
        assertEquals("README", openApi.getExternalDocs().getDescription());
    }
}
