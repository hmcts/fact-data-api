package uk.gov.hmcts.reform.fact.data.api.audit;

import java.util.Optional;
import java.util.UUID;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class AuditUserContext {

    private final ThreadLocal<UUID> userId = new ThreadLocal<>();
    private final ThreadLocal<Boolean> auditSuppressed = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public void setUserId(UUID userId) {
        this.userId.set(userId);
    }

    public Optional<UUID> getUserId() {
        return Optional.ofNullable(userId.get());
    }

    public UUID requireUserId() {
        return getUserId().orElseThrow(
            () -> new IllegalStateException("No audit user id is available for the current request")
        );
    }

    public void suppressAudit() {
        auditSuppressed.set(Boolean.TRUE);
    }

    public boolean isAuditSuppressed() {
        return Boolean.TRUE.equals(auditSuppressed.get());
    }

    public void clear() {
        userId.remove();
        auditSuppressed.remove();
    }
}
