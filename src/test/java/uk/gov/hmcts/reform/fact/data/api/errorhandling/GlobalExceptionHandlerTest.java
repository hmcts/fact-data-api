package uk.gov.hmcts.reform.fact.data.api.errorhandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidFileException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.TranslationNotFoundException;

import java.util.Collections;
import java.util.Map;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final String TEST_MESSAGE = "This is a test message";

    // Temp setup to allow tests to pass
    @MockitoBean
    private RateLimiterRegistry rateLimiterRegistry;

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Mock
    MethodArgumentNotValidException methodArgumentNotValidException;

    @Test
    void testHandleNotFoundException() {
        NotFoundException ex = new NotFoundException(TEST_MESSAGE);
        ExceptionResponse response = handler.handle(ex);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo(TEST_MESSAGE);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void testHandleTranslationNotFoundException() {
        TranslationNotFoundException ex = new TranslationNotFoundException(TEST_MESSAGE);
        ExceptionResponse response = handler.handle(ex);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo(TEST_MESSAGE);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void testHandleConstraintViolationException() {
        ConstraintViolationException ex =
            new ConstraintViolationException(TEST_MESSAGE, Collections.emptySet());
        ExceptionResponse response = handler.handle(ex);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Invalid input");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void testHandleMethodArgumentNotValidException() {
        FieldError fieldError = new FieldError("object", "field", TEST_MESSAGE);
        BeanPropertyBindingResult bindingResult =
            new BeanPropertyBindingResult(new Object(), "object");
        bindingResult.addError(fieldError);

        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);

        Map<String, String> response = handler.handle(methodArgumentNotValidException);

        assertThat(response).isNotNull();
        assertThat(response).containsKey("field");
        assertThat(response.get("field")).isEqualTo(TEST_MESSAGE);
    }

    @Test
    void testHandleHttpMessageNotReadableException() {
        String exceptionMessage =
            "Cannot deserialize value of type `java.util.UUID` from String \"hello\"";
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn(exceptionMessage);

        ExceptionResponse response = handler.handle(ex);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("Invalid request body");
        assertThat(response.getMessage()).contains(exceptionMessage);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void testHandleInvalidFileException() {
        InvalidFileException ex = new InvalidFileException(TEST_MESSAGE);
        ExceptionResponse response = handler.handle(ex);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo(TEST_MESSAGE);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void testHandleMaxUploadSizeExceededException() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(2097152L);
        ExceptionResponse response = handler.handle(ex);

        assertThat(response).isNotNull();
        assertThat(response.getMessage())
            .isEqualTo("Uploaded file size exceeds the maximum allowed limit of 2MB.");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void testHandleMethodArgumentTypeMismatchException() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
            "abc", Boolean.class, "includeClosed", null, new IllegalArgumentException("invalid boolean")
        );

        ExceptionResponse response = handler.handle(ex);

        assertThat(response).isNotNull();
        assertThat(response.getMessage())
            .contains("Invalid value for parameter 'includeClosed'")
            .contains("abc")
            .contains("Boolean");
        assertThat(response.getTimestamp()).isNotNull();
    }
}
