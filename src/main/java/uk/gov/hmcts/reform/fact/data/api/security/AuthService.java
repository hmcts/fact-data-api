package uk.gov.hmcts.reform.fact.data.api.security;

import uk.gov.hmcts.reform.fact.data.api.audit.AuditUserContext;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.UserRepository;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.reform.fact.data.api.config.properties.AuthConfigurationProperties;

@Service("authService")
@RequiredArgsConstructor
public class AuthService {

    public static final String USER_ID_HEADER = "X-User-Id";

    static final String PREFIX = "APPROLE_";
    static final String ROLE_ADMIN = "Role.Fact.Admin";
    static final String ROLE_VIEWER = "Role.Fact.Viewer";
    static final String ROLE_PRL = "Role.Fact.Prl";
    static final String ROLE_C100 = "Role.Fact.C100";
    static final String ROLE_CMC = "Role.Fact.Cmc";
    private static final char URI_PATH_DELIMITER = '/';

    private final AuthConfigurationProperties authConfigurationProperties;
    private final ObjectProvider<AuditUserContext> auditUserContextProvider;
    private final ObjectProvider<UserRepository> userRepositoryProvider;

    public boolean canView() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
           .map(this::findViewerRole)
           .isPresent();
    }

    public boolean isAdmin() {
        boolean isAdmin = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(this::findAdminRole)
            .isPresent();

        if (isAdmin) {
            setAuditUserContext();
        }

        return isAdmin;
    }

    public boolean isPrl() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(this::findPrlRole)
            .isPresent();
    }

    public boolean isC100() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(this::findC100Role)
            .isPresent();
    }

    public boolean isCmc() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(this::findCmcRole)
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

    private String findPrlRole(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
            .filter((PREFIX + ROLE_PRL)::equals).findFirst().orElse(null);
    }

    private String findC100Role(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
            .filter((PREFIX + ROLE_C100)::equals).findFirst().orElse(null);
    }

    private String findCmcRole(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
            .filter((PREFIX + ROLE_CMC)::equals).findFirst().orElse(null);
    }

    private void setAuditUserContext() {
        getCurrentRequest().ifPresent(request -> {
            if (isAdminEndpointWithoutUserHeader(request)) {
                Optional.ofNullable(auditUserContextProvider.getIfAvailable())
                    .ifPresent(AuditUserContext::suppressAudit);
                return;
            }

            String userIdHeader = request.getHeader(USER_ID_HEADER);
            if (userIdHeader == null || userIdHeader.isBlank()) {
                throw new IllegalArgumentException(USER_ID_HEADER + " header is required for admin requests");
            }

            UUID userId = parseUserId(userIdHeader);
            UserRepository repository = userRepositoryProvider.getObject();
            if (!repository.existsById(userId)) {
                throw new NotFoundException("No user found for user id: " + userId);
            }

            auditUserContextProvider.getObject().setUserId(userId);
        });
    }

    private UUID parseUserId(String userIdHeader) {
        try {
            return UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid " + USER_ID_HEADER + " header: " + userIdHeader, ex);
        }
    }

    private Optional<HttpServletRequest> getCurrentRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
            .filter(ServletRequestAttributes.class::isInstance)
            .map(ServletRequestAttributes.class::cast)
            .map(ServletRequestAttributes::getRequest);
    }

    private boolean isAdminEndpointWithoutUserHeader(HttpServletRequest request) {
        String method = request.getMethod();
        String requestUri = trimTrailingPathDelimiter(request.getRequestURI());
        AuthConfigurationProperties.UserHeaderBypass userHeaderBypass =
            authConfigurationProperties.getUserHeaderBypass();
        Set<String> postEndpointsWithoutUserHeader = parseConfiguredPaths(userHeaderBypass.getPostEndpoints());
        Set<String> deleteEndpointsWithoutUserHeader = parseConfiguredPaths(userHeaderBypass.getDeleteEndpoints());
        String putEndpointPrefix = trimTrailingPathDelimiter(userHeaderBypass.getPutPrefix());
        if (requestUri == null) {
            return false;
        }

        return ("POST".equals(method) && postEndpointsWithoutUserHeader.contains(requestUri))
            || ("PUT".equals(method)
            && putEndpointPrefix != null
            && requestUri.startsWith(putEndpointPrefix + URI_PATH_DELIMITER))
            || ("DELETE".equals(method) && deleteEndpointsWithoutUserHeader.contains(requestUri));
    }

    private Set<String> parseConfiguredPaths(String configuredPaths) {
        return Arrays.stream(configuredPaths.split(","))
            .map(String::trim)
            .filter(path -> !path.isEmpty())
            .map(this::trimTrailingPathDelimiter)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private String trimTrailingPathDelimiter(String requestUri) {
        if (requestUri == null) {
            return null;
        }

        int normalizedLength = requestUri.length();
        while (normalizedLength > 1 && requestUri.charAt(normalizedLength - 1) == URI_PATH_DELIMITER) {
            normalizedLength--;
        }

        return normalizedLength == requestUri.length()
            ? requestUri
            : requestUri.substring(0, normalizedLength);
    }
}
