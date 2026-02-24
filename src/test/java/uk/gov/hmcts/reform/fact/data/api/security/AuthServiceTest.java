package uk.gov.hmcts.reform.fact.data.api.security;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthServiceTest {

    AuthService authService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        authService = new AuthService();
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
    void isViewerOrAdminReturnsFalseWhenAnyNonAdminOrViewerIsPresent() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
            "test",
            "test",
            "APPROLE_Role.Made.Up.Nonsense"
        ));

        assertThat(authService.canView()).isFalse();
        assertThat(authService.isAdmin()).isFalse();
    }
}
