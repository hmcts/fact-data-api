package uk.gov.hmcts.reform.fact.data.api.security;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.fact.data.api.audit.AuditUserContext;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.UserRepository;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Mock
    private AuditUserContext auditUserContext;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectProvider<AuditUserContext> auditUserContextObjectProvider;

    @Mock
    private ObjectProvider<UserRepository> userRepositoryObjectProvider;

    AuthService authService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        lenient().when(auditUserContextObjectProvider.getObject()).thenReturn(auditUserContext);
        lenient().when(auditUserContextObjectProvider.getIfAvailable()).thenReturn(auditUserContext);
        lenient().when(userRepositoryObjectProvider.getObject()).thenReturn(userRepository);
        authService = new AuthService(auditUserContextObjectProvider, userRepositoryObjectProvider);
    }

    @Test
    void isViewerReturnsTrueWhenRoleIsPresent() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
            "test",
            "test",
            AuthService.PREFIX + AuthService.ROLE_VIEWER
        ));

        assertThat(authService.canView()).isTrue();
    }

    @Test
    void isViewerReturnsFalseWhenRoleIsNotPresent() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
            "test",
            "test"
        ));

        assertThat(authService.canView()).isFalse();
    }

    @Test
    void isViewerReturnsTrueWhenAdminRoleIsPresent() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
            "test",
            "test",
            AuthService.PREFIX + AuthService.ROLE_ADMIN
        ));
        assertThat(authService.canView()).isTrue();
    }

    @Test
    void isViewerReturnsTrueWhenBothViewerAndAdminRolesArePresent() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
            "test",
            "test",
            AuthService.PREFIX + AuthService.ROLE_ADMIN,
            AuthService.PREFIX + AuthService.ROLE_VIEWER
        ));
        assertThat(authService.canView()).isTrue();
    }

    @Test
    void isAdminReturnsTrueWhenAdminRoleIsPresent() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
            "test",
            "test",
            AuthService.PREFIX + AuthService.ROLE_ADMIN
        ));
        assertThat(authService.isAdmin()).isTrue();
    }

    @Test
    void isAdminReturnsTrueWhenBothViewerAndAdminRolesArePresent() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
            "test",
            "test",
            AuthService.PREFIX + AuthService.ROLE_ADMIN,
            AuthService.PREFIX + AuthService.ROLE_VIEWER
        ));

        assertThat(authService.isAdmin()).isTrue();
    }

    @Test
    void isAdminRequiresUserIdHeaderForRequest() {
        setAdminAuthentication();
        setRequest("POST", "/courts/v1", null);

        assertThatThrownBy(() -> authService.isAdmin())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("X-User-Id header is required for admin requests");
    }

    @Test
    void isAdminRequiresValidUserIdHeaderForRequest() {
        setAdminAuthentication();
        setRequest("POST", "/courts/v1", "not-a-uuid");

        assertThatThrownBy(() -> authService.isAdmin())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid X-User-Id header: not-a-uuid");
    }

    @Test
    void isAdminRequiresExistingUserIdHeaderForRequest() {
        setAdminAuthentication();
        setRequest("POST", "/courts/v1", USER_ID.toString());

        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> authService.isAdmin())
            .isInstanceOf(NotFoundException.class)
            .hasMessage("No user found for user id: " + USER_ID);
    }

    @Test
    void isAdminSetsAuditUserContextWhenUserExists() {
        setAdminAuthentication();
        setRequest("POST", "/courts/v1", USER_ID.toString());

        when(userRepository.existsById(USER_ID)).thenReturn(true);

        assertThat(authService.isAdmin()).isTrue();
        verify(auditUserContext).setUserId(USER_ID);
    }

    @Test
    void isAdminSuppressesAuditForCathLinkEndpoint() {
        assertAuditSuppressedFor("POST", "/courts/v1/link");
    }

    @Test
    void isAdminSuppressesAuditForCreateOrUpdateUserEndpoint() {
        assertAuditSuppressedFor("POST", "/user/v1");
    }

    @Test
    void isAdminSuppressesAuditForDeleteInactiveUsersEndpoint() {
        assertAuditSuppressedFor("DELETE", "/user/v1/retention");
    }

    @Test
    void isAdminSuppressesAuditForCathCourtDeletionEndpoint() {
        assertAuditSuppressedFor("PUT", "/courts/v1/link/MRD123");
    }

    @Test
    void isAdminRequiresUserIdHeaderWhenMethodDoesNotMatchBypassEndpoint() {
        setAdminAuthentication();
        setRequest("GET", "/courts/v1/link", null);

        assertThatThrownBy(() -> authService.isAdmin())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("X-User-Id header is required for admin requests");
    }

    private void assertAuditSuppressedFor(String method, String requestUri) {
        setAdminAuthentication();
        setRequest(method, requestUri, null);

        assertThat(authService.isAdmin()).isTrue();
        verify(auditUserContext).suppressAudit();
        verifyNoInteractions(userRepository);
    }

    @Test
    void isViewerOrAdminReturnsFalseWhenAnyNonAdminOrViewerIsPresent() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
            "test",
            "test",
            "APPROLE_Role.Made.Up.Nonsense"
        ));

        assertThat(authService.canView()).isFalse();
        assertThat(authService.isAdmin()).isFalse();
    }

    private void setAdminAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
            "test",
            "test",
            AuthService.PREFIX + AuthService.ROLE_ADMIN
        ));
    }

    private void setRequest(String method, String requestUri, String userIdHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, requestUri);
        if (userIdHeader != null) {
            request.addHeader(AuthService.USER_ID_HEADER, userIdHeader);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
