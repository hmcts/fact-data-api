package uk.gov.hmcts.reform.fact.data.api.controllers;

import static org.springframework.http.ResponseEntity.ok;

import uk.gov.hmcts.reform.fact.data.api.ratelimiting.RateLimitBucket4J;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rl-bucket4j/")
// This is our own annotation
@RateLimitBucket4J
public class RateLimitedBucket4JController {

    @GetMapping("/")
    @Operation(summary = "RateLimitedBucket4JController test endpoint")
    public ResponseEntity<String> bucket4jRLEndpoint() {
        return ok("You have NOT been rate limited by bucket4j!");
    }
}
