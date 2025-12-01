package uk.gov.hmcts.reform.fact.data.api.controllers;

import static org.springframework.http.ResponseEntity.ok;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rl-resilience4j/")
// This is a provided annotation
@RateLimiter(name = "admin")
@Slf4j
public class RateLimitedResilience4JController {

    @GetMapping("/")
    @Operation(summary = "RateLimitedResilience4JController test endpoint")
    public ResponseEntity<String> bucket4jRLEndpoint() {
        return ok("You have NOT been rate limited by resilience4j!");
    }
}
