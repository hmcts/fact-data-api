package uk.gov.hmcts.reform.fact.data.api.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import uk.gov.hmcts.reform.fact.data.api.entities.Lock;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.data.api.services.LockService;

import java.lang.reflect.Parameter;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Aspect that intercepts methods annotated with @LockTimeoutCheck.
 * Validates lock timeout before allowing the method to execute.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LockAspect {

    private final LockService lockService;

    @Value("${courtLock.timeout-minutes}")
    private long lockTimeoutMinutes;

    /**
     * Executes BEFORE any method annotated with @LockTimeoutCheck.
     * Extracts subjectType, subjectId, page, and userId from method parameters and validates lock.
     */
    @Before("@annotation(uk.gov.hmcts.reform.fact.data.api.aspect.annotations.LockTimeoutCheck)")
    public void validateLockTimeout(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        SubjectType subjectType = extractSubjectType(parameters, args, "subjectType");
        UUID subjectId = extractUuid(parameters, args, "subjectId");
        Page page = extractPage(parameters, args, "page");
        UUID userId = extractUuid(parameters, args, "userId");

        Optional<Lock> lock = lockService.getPageLock(subjectType, subjectId, page);
        if (lock.isEmpty() || lock.get().getUserId().equals(userId)) {
            return; // No lock or same user owns it
        }

        long minutesLocked = Duration.between(lock.get().getLockAcquired(), ZonedDateTime.now()).toMinutes();

        if (minutesLocked < lockTimeoutMinutes) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                String.format("Page locked by another user (%d/%d min)", minutesLocked, lockTimeoutMinutes));
        }

        lockService.deleteLock(subjectType, subjectId, page);
    }

    /**
     * Executes BEFORE any method annotated with @LockCleanupCheck.
     */
    @Before("@annotation(uk.gov.hmcts.reform.fact.data.api.aspect.annotations.LockCleanupCheck)")
    public void lockCleanup(JoinPoint joinPoint) {
        try {
            this.lockService.deleteExpiredLocks();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    /**
     * Extracts SubjectType parameter by name from method arguments.
     */
    private SubjectType extractSubjectType(Parameter[] parameters, Object[] args, String paramName) {
        for (int i = 0; i < parameters.length; i++) {
            if (isNamedParameter(parameters[i], paramName)) {
                Object value = args[i];
                return value instanceof String valueStr
                    ? SubjectType.valueOf(valueStr.toUpperCase())
                    : (SubjectType) value;
            }
        }
        throw new IllegalArgumentException("Required parameter not found: " + paramName);
    }

    /**
     * Extracts UUID parameter by name from method arguments.
     */
    private UUID extractUuid(Parameter[] parameters, Object[] args, String paramName) {
        for (int i = 0; i < parameters.length; i++) {
            if (isNamedParameter(parameters[i], paramName)) {
                Object value = args[i];
                return value instanceof String valueStr
                    ? UUID.fromString(valueStr)
                    : (UUID) value;
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
                return value instanceof String valueStr
                    ? Page.valueOf(valueStr.toUpperCase())
                    : (Page) value;
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
