package uk.gov.hmcts.reform.fact.data.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
            .components(new Components().addSecuritySchemes(
                BEARER_AUTH_SECURITY_SCHEME,
                new SecurityScheme()
                    .name(BEARER_AUTH_SECURITY_SCHEME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            ));
    }

}
