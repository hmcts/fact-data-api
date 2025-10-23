package uk.gov.hmcts.reform.fact.data.api.services;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("authService")
public class AuthService {

    private static final String PREFIX = "APPROLE_";
    private static final String ROLE_ADMIN = "Role.Fact.Admin";
    private static final String ROLE_USER = "Role.Fact.User";

    public boolean isUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(this::findUserRole)
            .isPresent();
    }

    public boolean isAdmin() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(this::findAdminRole)
            .isPresent();
    }

    private String findUserRole(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
            .filter((PREFIX+ROLE_ADMIN)::equals).filter((PREFIX+ROLE_USER)::equals)
            .findFirst().orElse(null);
    }

    private String findAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
            .filter((PREFIX+ROLE_ADMIN)::equals).findFirst().orElse(null);
    }
}
