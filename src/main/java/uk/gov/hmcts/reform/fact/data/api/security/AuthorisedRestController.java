package uk.gov.hmcts.reform.fact.data.api.security;

import uk.gov.hmcts.reform.fact.data.api.config.OpenAPIConfiguration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Convenience annotation that brings together the boilerplate annotations required to set up a {@link RestController}
 * with a requirement for default security, both in terms of operation, and documentation (swagger).
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SecurityRequirement(name = OpenAPIConfiguration.BEARER_AUTH_SECURITY_SCHEME)
@PreAuthorize("@authService.isViewer()")
@RestController
public @interface AuthorisedRestController {
}
