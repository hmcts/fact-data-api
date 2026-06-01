package uk.gov.hmcts.reform.fact.data.api.audit;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class AuditUserContextCleanupFilter extends OncePerRequestFilter {

    private final ObjectProvider<AuditUserContext> auditUserContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            AuditUserContext context = auditUserContext.getIfAvailable();
            if (context != null) {
                context.clear();
            }
        }
    }
}
