package uk.gov.hmcts.reform.fact.data.api.errorhandling;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidFileException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.TranslationNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final String TEST_MESSAGE = "This is a test message";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

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
    void testHandleConstraintViolationExceptionWithValidUuidAnnotation() {
        ConstraintViolation<?> violation = createConstraintViolation(
            mock(ValidUUID.class), "bad-uuid", null
        );

        ConstraintViolationException ex =
            new ConstraintViolationException(TEST_MESSAGE, Set.of(violation));

        ExceptionResponse response = handler.handle(ex);

        assertThat(response.getMessage()).isEqualTo("Invalid UUID supplied: bad-uuid");
    }

    @Test
    void testHandleConstraintViolationExceptionWithBlankInvalidValue() {
        ConstraintViolation<?> violation = createConstraintViolation(
            mock(Annotation.class), "   ", "Value is mandatory"
        );

        ConstraintViolationException ex =
            new ConstraintViolationException(TEST_MESSAGE, Set.of(violation));

        ExceptionResponse response = handler.handle(ex);

        assertThat(response.getMessage()).isEqualTo("Value is mandatory");
    }

    @Test
    void testHandleConstraintViolationExceptionWithNonBlankInvalidValue() {
        ConstraintViolation<?> violation = createConstraintViolation(
            mock(Annotation.class), "ABC123", "Invalid format"
        );

        ConstraintViolationException ex =
            new ConstraintViolationException(TEST_MESSAGE, Set.of(violation));

        ExceptionResponse response = handler.handle(ex);

        assertThat(response.getMessage()).isEqualTo("Invalid format: ABC123");
    }

    @SuppressWarnings("unchecked")
    private ConstraintViolation<?> createConstraintViolation(
        Annotation annotation, Object invalidValue, String message) {

        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        ConstraintDescriptor<Annotation> descriptor =
            (ConstraintDescriptor<Annotation>) mock(ConstraintDescriptor.class);
        when(violation.getConstraintDescriptor()).thenReturn((ConstraintDescriptor) descriptor);
        when(descriptor.getAnnotation()).thenReturn(annotation);
        when(violation.getInvalidValue()).thenReturn(invalidValue);
        if (message != null) {
            when(violation.getMessage()).thenReturn(message);
        }
        return violation;
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
