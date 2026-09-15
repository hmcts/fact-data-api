package uk.gov.hmcts.reform.fact.data.api.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import uk.gov.hmcts.reform.fact.data.api.security.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringSecurityConfigurationTest {

    @Mock
    private AuthService authService;

    @Mock
    private HttpSecurity httpSecurity;

    @Mock
    private DefaultSecurityFilterChain securityFilterChain;

    @Test
    void apiSecurityFilterChainBuildsWithAuthorizationAndCsrfCustomizers() {
        SpringSecurityConfiguration configuration = new SpringSecurityConfiguration(authService);

        @SuppressWarnings("unchecked")
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authRegistry =
            (AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry)
                mock(AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry.class,
                    org.mockito.Answers.RETURNS_DEEP_STUBS);

        @SuppressWarnings("unchecked")
        CsrfConfigurer<HttpSecurity> csrfConfigurer = mock(CsrfConfigurer.class, org.mockito.Answers.RETURNS_SELF);

        doReturn(httpSecurity).when(httpSecurity).with(any(), any());
        when(httpSecurity.authorizeHttpRequests(any())).thenAnswer(invocation -> {
            Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>
                customizer = invocation.getArgument(0);
            customizer.customize(authRegistry);
            return httpSecurity;
        });
        when(httpSecurity.csrf(any())).thenAnswer(invocation -> {
            Customizer<CsrfConfigurer<HttpSecurity>> customizer = invocation.getArgument(0);
            customizer.customize(csrfConfigurer);
            return httpSecurity;
        });
        when(httpSecurity.build()).thenReturn(securityFilterChain);

        SecurityFilterChain result = configuration.apiSecurityFilterChain(httpSecurity);

        assertThat(result).isSameAs(securityFilterChain);
        verify(authRegistry).requestMatchers("/");
        verify(csrfConfigurer).ignoringRequestMatchers("/testing-support/**");
    }
}




