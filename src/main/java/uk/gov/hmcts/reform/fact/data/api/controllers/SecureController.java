package uk.gov.hmcts.reform.fact.data.api.controllers;

import static org.springframework.http.ResponseEntity.ok;

import uk.gov.hmcts.reform.fact.data.api.config.OpenAPIConfiguration;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/secure")
// might neeed "APPROLE_" prefix?
//@RolesAllowed("Role.Fact.User") // how I assume we'd do it normally...
public class SecureController {

    @GetMapping("/admin")
    // might neeed "APPROLE_" prefix?
    //@RolesAllowed("Role.Fact.Admin") // how I assume we'd do it normally...
    @PreAuthorize("@authService.isAdmin()")
    @SecurityRequirement(name = OpenAPIConfiguration.BEARER_AUTH_SECURITY_SCHEME)
    public ResponseEntity<String> testAdmin() {
        return ok("admin test");
    }

    @GetMapping("/user")
    @PreAuthorize("@authService.isUser()")
    @SecurityRequirement(name = OpenAPIConfiguration.BEARER_AUTH_SECURITY_SCHEME)
    public ResponseEntity<String> testUser() {
        return ok("user test");
    }

    @GetMapping("/authenticated")
    @PreAuthorize("@authService.isAuthenticated()")
    @SecurityRequirement(name = OpenAPIConfiguration.BEARER_AUTH_SECURITY_SCHEME)
    public ResponseEntity<String> testAuthenticated() {
        return ok("authenticated test");
    }
}
