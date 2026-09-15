package uk.gov.hmcts.reform.fact.data.api.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void customizerAddsUserIdHeaderToNonTestingEndpointsOnly() {
        OpenAPI openApi = new OpenAPI()
            .path("/courts/v1", new PathItem().get(new Operation()))
            .path("/testing-support/demo", new PathItem().post(new Operation()));

        new OpenAPIConfiguration().openApiCustomizer().customise(openApi);

        assertThat(openApi.getPaths().get("/courts/v1").getGet().getParameters())
            .extracting(Parameter::getName)
            .contains("X-User-Id");
        assertThat(openApi.getPaths().get("/testing-support/demo").getPost().getParameters()).isNull();
    }
}
