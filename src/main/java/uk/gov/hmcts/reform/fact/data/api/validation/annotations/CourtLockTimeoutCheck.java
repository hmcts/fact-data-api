package uk.gov.hmcts.reform.fact.data.api.validation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates if a court page lock has exceeded timeout before allowing operation.
 * Applied to controller methods that modify locked resources.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CourtLockTimeoutCheck {
}
