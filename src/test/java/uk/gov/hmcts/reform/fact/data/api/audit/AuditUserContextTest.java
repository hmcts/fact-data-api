package uk.gov.hmcts.reform.fact.data.api.audit;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditUserContextTest {

    @Test
    void storesAndRetrievesUserId() {
        AuditUserContext context = new AuditUserContext();
        UUID userId = UUID.randomUUID();

        context.setUserId(userId);

        assertThat(context.getUserId()).contains(userId);
        assertThat(context.requireUserId()).isEqualTo(userId);
    }

    @Test
    void requireUserIdThrowsWhenMissing() {
        AuditUserContext context = new AuditUserContext();

        assertThatThrownBy(context::requireUserId)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No audit user id is available for the current request");
    }

    @Test
    void suppressAuditFlagDefaultsToFalseAndCanBeEnabled() {
        AuditUserContext context = new AuditUserContext();

        assertThat(context.isAuditSuppressed()).isFalse();

        context.suppressAudit();

        assertThat(context.isAuditSuppressed()).isTrue();
    }

    @Test
    void clearRemovesAllContextState() {
        AuditUserContext context = new AuditUserContext();
        context.setUserId(UUID.randomUUID());
        context.suppressAudit();

        context.clear();

        assertThat(context.getUserId()).isEmpty();
        assertThat(context.isAuditSuppressed()).isFalse();
    }
}

