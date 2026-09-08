package uk.gov.hmcts.reform.fact.data.api.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditUserContextCleanupFilterTest {

    @Mock
    private ObjectProvider<AuditUserContext> auditUserContextProvider;

    @Mock
    private AuditUserContext auditUserContext;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private AuditUserContextCleanupFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuditUserContextCleanupFilter(auditUserContextProvider);
    }

    @Test
    void clearsAuditContextAfterSuccessfulFilterChain() throws Exception {
        when(auditUserContextProvider.getIfAvailable()).thenReturn(auditUserContext);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(auditUserContext).clear();
    }

    @Test
    void doesNotAttemptToClearWhenContextMissing() throws Exception {
        when(auditUserContextProvider.getIfAvailable()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(auditUserContextProvider).getIfAvailable();
        verifyNoInteractions(auditUserContext);
    }

    @Test
    void clearsAuditContextWhenFilterChainThrows() throws Exception {
        when(auditUserContextProvider.getIfAvailable()).thenReturn(auditUserContext);
        doThrow(new ServletException("boom")).when(filterChain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
            .isInstanceOf(ServletException.class)
            .hasMessage("boom");

        verify(auditUserContext).clear();
    }
}

