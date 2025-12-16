package uk.gov.hmcts.reform.fact.data.api.controllers;

import static org.springframework.http.ResponseEntity.ok;

import uk.gov.hmcts.reform.fact.data.api.config.OpenAPIConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Default endpoints per application.
 */
@RestController
@SecurityRequirement(name = OpenAPIConfiguration.BEARER_AUTH_SECURITY_SCHEME)
public class RootController {

    /**
     * Root GET endpoint.
     *
     * <p>Azure application service has a hidden feature of making requests to root endpoint when
     * "Always On" is turned on.
     * This is the endpoint to deal with that and therefore silence the unnecessary 404s as a response code.
     *
     * @return Welcome message from the service.
     */
    @GetMapping("/")
    @Operation(summary = "The default root for the application")
    public ResponseEntity<String> welcome() {
        return ok("Welcome to fact-data-api");
    }
}
