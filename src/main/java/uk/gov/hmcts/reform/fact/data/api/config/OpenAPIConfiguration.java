package uk.gov.hmcts.reform.fact.data.api.config;

import java.util.Map;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfiguration {

    public static final String BEARER_AUTH_SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info().title("Fact Data API")
                      .description("API for all operations relating to the Find a Court or Tribunal Service")
                      .version("v0.0.1")
                      .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
            .externalDocs(new ExternalDocumentation()
                              .description("README")
                              .url("https://github.com/hmcts/fact-data-api"))
            .components(new Components()
                            .addSecuritySchemes(
                                BEARER_AUTH_SECURITY_SCHEME,
                                new SecurityScheme()
                                    .name(BEARER_AUTH_SECURITY_SCHEME)
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                            ));
    }

    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi ->
            openApi.getPaths().entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith("/testing-support/")
                    && !entry.getKey().startsWith("/migration/"))
                .map(Map.Entry::getValue)
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .forEach(
                    operation -> operation.addParametersItem(
                        new HeaderParameter()
                            .name("X-User-Id")
                            .description("The ID of the user making the request")
                            .required(true)
                            .schema(new io.swagger.v3.oas.models.media.StringSchema())
                    )
                );
    }

}
