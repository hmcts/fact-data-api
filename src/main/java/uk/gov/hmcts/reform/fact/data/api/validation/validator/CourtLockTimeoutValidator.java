package uk.gov.hmcts.reform.fact.data.api.validation.validator;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLock;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.data.api.services.CourtLockService;

import java.lang.reflect.Parameter;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Aspect that intercepts methods annotated with @ValidateLockTimeout.
 * Validates lock timeout before allowing the method to execute.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class CourtLockTimeoutValidator {

    private final CourtLockService courtLockService;

    @Value("${courtLock.timeout-minutes}")
    private long lockTimeoutMinutes;

    /**
     * Executes BEFORE any method annotated with @ValidateLockTimeout.
     * Extracts courtId, page, and userId from method parameters and validates lock.
     */
    @Before("@annotation(uk.gov.hmcts.reform.fact.data.api.validation.annotations.CourtLockTimeoutCheck)")
    public void validateLockTimeout(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        UUID courtId = extractUuid(parameters, args, "courtId");
        Page page = extractPage(parameters, args, "page");
        UUID userId = extractUuid(parameters, args, "userId");

        Optional<CourtLock> lock = courtLockService.getPageLock(courtId, page);
        if (lock.isEmpty() || lock.get().getUserId().equals(userId)) {
            return; // No lock or same user owns it
        }

        long minutesLocked = Duration.between(lock.get().getLockAcquired(), ZonedDateTime.now()).toMinutes();

        if (minutesLocked < lockTimeoutMinutes) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                String.format("Page locked by another user (%d/%d min)", minutesLocked, lockTimeoutMinutes));
        }

        courtLockService.deleteLock(courtId, page);
    }

    /**
     * Extracts UUID parameter by name from method arguments.
     */
    private UUID extractUuid(Parameter[] parameters, Object[] args, String paramName) {
        for (int i = 0; i < parameters.length; i++) {
            if (isNamedParameter(parameters[i], paramName)) {
                Object value = args[i];
                return value instanceof String ? UUID.fromString((String) value) : (UUID) value;
            }
        }
        throw new IllegalArgumentException("Required parameter not found: " + paramName);
    }

    /**
     * Extracts Page enum parameter by name from method arguments.
     */
    private Page extractPage(Parameter[] parameters, Object[] args, String paramName) {
        for (int i = 0; i < parameters.length; i++) {
            if (isNamedParameter(parameters[i], paramName)) {
                Object value = args[i];
                return value instanceof String ? Page.valueOf(((String) value).toUpperCase()) : (Page) value;
            }
        }
        throw new IllegalArgumentException("Required parameter not found: " + paramName);
    }

    /**
     * Checks if a parameter matches the given name by checking annotations.
     */
    private boolean isNamedParameter(Parameter parameter, String name) {
        PathVariable pathVar = parameter.getAnnotation(PathVariable.class);
        if (pathVar != null && (pathVar.value().equals(name) || pathVar.name().equals(name))) {
            return true;
        }

        RequestParam reqParam = parameter.getAnnotation(RequestParam.class);
        if (reqParam != null && (reqParam.value().equals(name) || reqParam.name().equals(name))) {
            return true;
        }

        return parameter.getName().equals(name);
    }
}
