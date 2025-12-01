package uk.gov.hmcts.reform.fact.data.api.controllers;

import static org.springframework.http.ResponseEntity.ok;

import uk.gov.hmcts.reform.fact.data.api.ratelimiting.RateLimitBucket4J;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rl-general/")
public class RateLimitedController {

    @GetMapping("/b4j-default")
    @Operation(summary = "RateLimitedController test endpoint (bucket4j - default)")
    @RateLimitBucket4J
    public ResponseEntity<String> bucket4jDefaultEndpoint() {
        return ok("You have NOT been rate limited by the default bucket (bucket4j)!");
    }

    @GetMapping("/b4j-admin")
    @Operation(summary = "RateLimitedController test endpoint (bucket4j - admin)")
    @RateLimitBucket4J(bucket = "admin")
    public ResponseEntity<String> bucket4jAdminEndpoint() {
        return ok("You have NOT been rate limited by the admin bucket (bucket4j)!");
    }

    @GetMapping("/b4j-public")
    @Operation(summary = "RateLimitedController test endpoint (bucket4j - public)")
    @RateLimitBucket4J(bucket = "public")
    public ResponseEntity<String> bucket4jPublicEndpoint() {
        return ok("You have NOT been rate limited by the public bucket (bucket4j)!");
    }

    @GetMapping("/r4j-default")
    @Operation(summary = "RateLimitedController test endpoint (resilience4J - default)")
    @RateLimiter(name = "default")
    public ResponseEntity<String> resilience4JDefaultEndpoint() {
        return ok("You have NOT been rate limited by the default limiter (resilience4J)!");
    }

    @GetMapping("/r4j-admin")
    @Operation(summary = "RateLimitedController test endpoint (resilience4J - admin)")
    @RateLimiter(name = "admin")
    public ResponseEntity<String> resilience4JAdminEndpoint() {
        return ok("You have NOT been rate limited by the admin limiter (resilience4J)!");
    }

    @GetMapping("/r4j-public")
    @Operation(summary = "RateLimitedController test endpoint (resilience4J - public)")
    @RateLimiter(name = "public")
    public ResponseEntity<String> resilience4JPublicEndpoint() {
        return ok("You have NOT been rate limited by the public limiter (resilience4J)!");
    }
}
