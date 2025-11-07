package uk.gov.hmcts.reform.fact.data.api.validator;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLock;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.data.api.services.CourtLockService;
import uk.gov.hmcts.reform.fact.data.api.validation.validator.CourtLockTimeoutValidator;

import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtLockTimeoutValidatorTest {

    @Mock
    private CourtLockService courtLockService;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private CourtLockTimeoutValidator validator;

    private final UUID courtId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();
    private final Page page = Page.COURT;

    @BeforeEach
    void setUp() {
        validator = new CourtLockTimeoutValidator(courtLockService);
        ReflectionTestUtils.setField(validator, "lockTimeoutMinutes", 60L);
    }

    @Test
    @DisplayName("Should allow operation when no lock exists")
    void shouldAllowWhenNoLockExists() {
        // Given
        setupJoinPoint(courtId, page, userId);
        when(courtLockService.getPageLock(courtId, page)).thenReturn(Optional.empty());

        // When & Then
        assertDoesNotThrow(() -> validator.validateLockTimeout(joinPoint));
        verify(courtLockService, never()).deleteLock(any(), any());
    }

    @Test
    @DisplayName("Should allow operation when same user owns the lock")
    void shouldAllowWhenSameUserOwnsLock() {
        // Given
        setupJoinPoint(courtId, page, userId);
        CourtLock lock = createLock(userId, ZonedDateTime.now().minusMinutes(30));
        when(courtLockService.getPageLock(courtId, page)).thenReturn(Optional.of(lock));

        // When & Then
        assertDoesNotThrow(() -> validator.validateLockTimeout(joinPoint));
        verify(courtLockService, never()).deleteLock(any(), any());
    }

    @Test
    @DisplayName("Should throw 409 CONFLICT when lock is valid and held by another user")
    void shouldThrowConflictWhenLockIsValid() {
        // Given
        setupJoinPoint(courtId, page, userId);
        CourtLock lock = createLock(otherUserId, ZonedDateTime.now().minusMinutes(30));
        when(courtLockService.getPageLock(courtId, page)).thenReturn(Optional.of(lock));

        // When
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> validator.validateLockTimeout(joinPoint)
        );

        // Then
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Page locked by another user (30/60 min)", exception.getReason());
        verify(courtLockService, never()).deleteLock(any(), any());
    }

    @Test
    @DisplayName("Should delete stale lock and allow operation when lock has exceeded timeout")
    void shouldDeleteStaleLockAndAllow() {
        // Given
        setupJoinPoint(courtId, page, userId);
        CourtLock lock = createLock(otherUserId, ZonedDateTime.now().minusMinutes(61));
        when(courtLockService.getPageLock(courtId, page)).thenReturn(Optional.of(lock));

        // When & Then
        assertDoesNotThrow(() -> validator.validateLockTimeout(joinPoint));
        verify(courtLockService).deleteLock(courtId, page);
    }

    @Test
    @DisplayName("Should delete lock exactly at timeout threshold")
    void shouldDeleteLockAtExactTimeout() {
        // Given
        setupJoinPoint(courtId, page, userId);
        CourtLock lock = createLock(otherUserId, ZonedDateTime.now().minusMinutes(60));
        when(courtLockService.getPageLock(courtId, page)).thenReturn(Optional.of(lock));

        // When & Then
        assertDoesNotThrow(() -> validator.validateLockTimeout(joinPoint));
        verify(courtLockService).deleteLock(courtId, page);
    }

    @Test
    @DisplayName("Should not delete lock just before timeout threshold")
    void shouldNotDeleteLockJustBeforeTimeout() {
        // Given
        setupJoinPoint(courtId, page, userId);
        CourtLock lock = createLock(otherUserId, ZonedDateTime.now().minusMinutes(59));
        when(courtLockService.getPageLock(courtId, page)).thenReturn(Optional.of(lock));

        // When
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> validator.validateLockTimeout(joinPoint)
        );

        // Then
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(courtLockService, never()).deleteLock(any(), any());
    }

    @Test
    @DisplayName("Should handle very old stale locks")
    void shouldHandleVeryOldStaleLocks() {
        // Given
        setupJoinPoint(courtId, page, userId);
        CourtLock lock = createLock(otherUserId, ZonedDateTime.now().minusHours(24));
        when(courtLockService.getPageLock(courtId, page)).thenReturn(Optional.of(lock));

        // When & Then
        assertDoesNotThrow(() -> validator.validateLockTimeout(joinPoint));
        verify(courtLockService).deleteLock(courtId, page);
    }

    @Test
    @DisplayName("Should respect custom timeout configuration")
    void shouldRespectCustomTimeout() {
        // Given
        ReflectionTestUtils.setField(validator, "lockTimeoutMinutes", 15L);
        setupJoinPoint(courtId, page, userId);
        CourtLock lock = createLock(otherUserId, ZonedDateTime.now().minusMinutes(20));
        when(courtLockService.getPageLock(courtId, page)).thenReturn(Optional.of(lock));

        // When & Then
        assertDoesNotThrow(() -> validator.validateLockTimeout(joinPoint));
        verify(courtLockService).deleteLock(courtId, page);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when courtId parameter is missing")
    void shouldThrowWhenCourtIdMissing() {
        // Given
        setupJoinPointWithoutCourtId(page, userId);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateLockTimeout(joinPoint)
        );
        assertEquals("Required parameter not found: courtId", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when page parameter is missing")
    void shouldThrowWhenPageMissing() {
        // Given
        setupJoinPointWithoutPage(courtId, userId);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateLockTimeout(joinPoint)
        );
        assertEquals("Required parameter not found: page", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when userId parameter is missing")
    void shouldThrowWhenUserIdMissing() {
        // Given
        setupJoinPointWithoutUserId(courtId, page);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateLockTimeout(joinPoint)
        );
        assertEquals("Required parameter not found: userId", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle courtId as String and convert to UUID")
    void shouldHandleCourtIdAsString() {
        // Given
        setupJoinPointWithStringCourtId(courtId.toString(), page, userId);
        when(courtLockService.getPageLock(courtId, page)).thenReturn(Optional.empty());

        // When & Then
        assertDoesNotThrow(() -> validator.validateLockTimeout(joinPoint));
    }

    @Test
    @DisplayName("Should handle page as String and convert to enum")
    void shouldHandlePageAsString() {
        // Given
        setupJoinPointWithStringPage(courtId, "COURT", userId);
        when(courtLockService.getPageLock(courtId, page)).thenReturn(Optional.empty());

        // When & Then
        assertDoesNotThrow(() -> validator.validateLockTimeout(joinPoint));
    }

    // Helper methods

    private CourtLock createLock(UUID userId, ZonedDateTime lockAcquired) {
        CourtLock lock = new CourtLock();
        lock.setUserId(userId);
        lock.setLockAcquired(lockAcquired);
        lock.setCourtId(courtId);
        lock.setPage(page);
        return lock;
    }

    private void setupJoinPoint(UUID courtId, Page page, UUID userId) {
        try {
            Method method = TestController.class.getMethod("testMethod", UUID.class, Page.class, UUID.class);
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.getArgs()).thenReturn(new Object[]{courtId, page, userId});
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupJoinPointWithStringCourtId(String courtId, Page page, UUID userId) {
        try {
            Method method = TestController.class.getMethod(
                "testMethodWithStringCourtId", String.class, Page.class, UUID.class);
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.getArgs()).thenReturn(new Object[]{courtId, page, userId});
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupJoinPointWithStringPage(UUID courtId, String page, UUID userId) {
        try {
            Method method = TestController.class.getMethod(
                "testMethodWithStringPage", UUID.class, String.class, UUID.class);
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.getArgs()).thenReturn(new Object[]{courtId, page, userId});
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupJoinPointWithoutCourtId(Page page, UUID userId) {
        try {
            Method method = TestController.class.getMethod("testMethodWithoutCourtId", Page.class, UUID.class);
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.getArgs()).thenReturn(new Object[]{page, userId});
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupJoinPointWithoutPage(UUID courtId, UUID userId) {
        try {
            Method method = TestController.class.getMethod("testMethodWithoutPage", UUID.class, UUID.class);
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.getArgs()).thenReturn(new Object[]{courtId, userId});
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupJoinPointWithoutUserId(UUID courtId, Page page) {
        try {
            Method method = TestController.class.getMethod("testMethodWithoutUserId", UUID.class, Page.class);
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);
            when(joinPoint.getArgs()).thenReturn(new Object[]{courtId, page});
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    // Test controller class to provide methods with proper annotations
    @SuppressWarnings("unused")
    static class TestController {
        public void testMethod(
            @PathVariable("courtId") UUID courtId,
            @PathVariable("page") Page page,
            @RequestParam("userId") UUID userId) {
        }

        public void testMethodWithStringCourtId(
            @PathVariable("courtId") String courtId,
            @PathVariable("page") Page page,
            @RequestParam("userId") UUID userId) {
        }

        public void testMethodWithStringPage(
            @PathVariable("courtId") UUID courtId,
            @PathVariable("page") String page,
            @RequestParam("userId") UUID userId) {
        }

        public void testMethodWithoutCourtId(
            @PathVariable("page") Page page,
            @RequestParam("userId") UUID userId) {
        }

        public void testMethodWithoutPage(
            @PathVariable("courtId") UUID courtId,
            @RequestParam("userId") UUID userId) {
        }

        public void testMethodWithoutUserId(
            @PathVariable("courtId") UUID courtId,
            @PathVariable("page") Page page) {
        }
    }
}

