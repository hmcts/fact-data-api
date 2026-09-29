package uk.gov.hmcts.reform.fact.data.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

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
            .addTagsItem(new Tag()
                             .name("apim")
                             .description("Operations available to the Azure API Management gateway"))
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
                .filter(entry -> !entry.getKey().startsWith("/testing-support/"))
                .flatMap(pathEntry -> pathEntry.getValue().readOperationsMap().values().stream())
                .filter(operation -> {
                    // Skip adding the header for APIM tagged operations
                    return operation.getTags() == null || operation.getTags().stream()
                        .noneMatch("apim"::equals);
                })
                .forEach(
                    operation -> operation.addParametersItem(
                        new HeaderParameter()
                            .name("X-User-Id")
                            .description("The ID of the user making the request")
                            .required(true)
                            .schema(new StringSchema())
                    )
                );
    }

    @Bean
    public GroupedOpenApi defaultOpenApi() {
        return GroupedOpenApi.builder()
            .group("default")
            .addOpenApiCustomizer(openApiCustomizer())
            .build();
    }

    @Bean
    public GroupedOpenApi groupedApimOpenApi() {
        return GroupedOpenApi.builder()
            .group("apim")
            .addOperationCustomizer((operation, handlerMethod) -> operation != null
                && operation.getTags() != null
                && operation.getTags().contains("apim")
                ? operation
                : null)
            .addOpenApiCustomizer(openApi -> {
                openApi.getPaths().entrySet()
                    .removeIf(entry -> entry.getValue().readOperations().isEmpty());

                openApi.getPaths().values()
                    .forEach(path -> path.readOperations()
                        .forEach(op -> op.setTags(List.of("apim"))));

                openApi.setTags(List.of(
                    new Tag().name("apim")
                        .description("Operations available to the Azure API Management gateway")
                ));

                openApi.setInfo(
                    new Info()
                        .title("Fact Data API - APIM")
                        .description("API for operations relating to the Find a Court or Tribunal Service exposed "
                            + "through Azure API Management")
                        .version("v0.0.1")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"))
                );
            })
            .build();
    }
}
