package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.fact.data.api.services.BlobStorageService;

import static org.springframework.http.ResponseEntity.ok;

/**
 * Default endpoints per application.
 */
@RestController
public class RootController {

    private final BlobStorageService blobStorageService;

    public RootController(BlobStorageService blobStorageService) {
        this.blobStorageService = blobStorageService;
    }

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

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        blobStorageService.logBlobInfo("test_1.png");
        return ok("Test endpoint is working");
    }
}
