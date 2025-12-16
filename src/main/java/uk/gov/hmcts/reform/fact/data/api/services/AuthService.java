package uk.gov.hmcts.reform.fact.data.api.services;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("authService")
@Slf4j
public class AuthService {

    private static final String PREFIX = "APPROLE_";
    private static final String ROLE_ADMIN = "Role.Fact.Admin";
    private static final String ROLE_VIEWER = "Role.Fact.Viewer";

    public boolean isViewer() {
        log.info("Checking if viewer is enabled");
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
           .map(this::findViewerRole)
           .isPresent();
    }

    public boolean isAdmin() {
        log.info("Checking if admin is enabled");
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(this::findAdminRole)
            .isPresent();
    }

    public boolean isAuthenticated() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .isPresent();
    }

    private String findViewerRole(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
            .filter(a -> a.equals(PREFIX + ROLE_VIEWER) || a.equals(PREFIX + ROLE_ADMIN))
            .findFirst().orElse(null);
    }

    private String findAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
            .filter((PREFIX + ROLE_ADMIN)::equals).findFirst().orElse(null);
    }
}
