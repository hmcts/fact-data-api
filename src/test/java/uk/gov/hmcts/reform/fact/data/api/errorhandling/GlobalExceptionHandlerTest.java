package uk.gov.hmcts.reform.fact.data.api.errorhandling;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Payload;
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
import org.springframework.web.multipart.MultipartException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidFileException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
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
    void testHandleCourtResourceNotFoundException() {
        CourtResourceNotFoundException ex = new CourtResourceNotFoundException(TEST_MESSAGE);
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
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        ConstraintDescriptor<?> descriptor = mock(ConstraintDescriptor.class);
        doReturn(descriptor).when(violation).getConstraintDescriptor();
        doReturn(validUuidAnnotation()).when(descriptor).getAnnotation();
        when(violation.getInvalidValue()).thenReturn("bad-uuid");

        ConstraintViolationException ex =
            new ConstraintViolationException("invalid", Set.of(violation));

        ExceptionResponse response = handler.handle(ex);

        assertThat(response.getMessage()).isEqualTo("Invalid UUID supplied: bad-uuid");
    }

    @Test
    void testHandleConstraintViolationExceptionWithNonUuidAnnotation() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        ConstraintDescriptor<?> descriptor = mock(ConstraintDescriptor.class);
        doReturn(descriptor).when(violation).getConstraintDescriptor();
        doReturn(new Annotation() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Annotation.class;
            }
        }).when(descriptor).getAnnotation();
        when(violation.getMessage()).thenReturn("Bad request");
        when(violation.getInvalidValue()).thenReturn("oops");

        ConstraintViolationException ex =
            new ConstraintViolationException("invalid", Set.of(violation));

        ExceptionResponse response = handler.handle(ex);

        assertThat(response.getMessage()).isEqualTo("Bad request: oops");
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
    void testHandleMethodArgumentNotValidExceptionWithDuplicateFieldsRetainsFirstMessage() {
        BeanPropertyBindingResult bindingResult =
            new BeanPropertyBindingResult(new Object(), "object");
        bindingResult.addError(new FieldError("object", "field", "first"));
        bindingResult.addError(new FieldError("object", "field", "second"));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        Map<String, String> response = handler.handle(ex);

        assertThat(response.get("field")).isEqualTo("first");
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

    @Test
    void testHandleMethodArgumentTypeMismatchExceptionWithUnknownExpectedType() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
            "123", null, "page", null, new IllegalArgumentException("fail")
        );

        ExceptionResponse response = handler.handle(ex);

        assertThat(response.getMessage())
            .contains("Invalid value for parameter 'page'")
            .contains("unknown");
    }

    @Test
    void testHandleMultipartExceptionWithProvidedContentType() {
        MultipartException ex = new MultipartException("Missing multipart boundary");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContentType()).thenReturn("multipart/form-data");

        ExceptionResponse response = handler.handle(ex, request);

        assertThat(response).isNotNull();
        assertThat(response.getMessage())
            .contains("Unsupported or malformed Content-Type 'multipart/form-data'")
            .contains("use 'multipart/form-data'")
            .contains("use 'application/json'");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void testHandleMultipartExceptionWithUnknownContentType() {
        MultipartException ex = new MultipartException("Not a multipart request");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContentType()).thenReturn(null);

        ExceptionResponse response = handler.handle(ex, request);

        assertThat(response).isNotNull();
        assertThat(response.getMessage())
            .contains("Unsupported or malformed Content-Type 'unknown'")
            .contains("use 'multipart/form-data'")
            .contains("use 'application/json'");
        assertThat(response.getTimestamp()).isNotNull();
    }

    private ValidUUID validUuidAnnotation() {
        return new ValidUUID() {
            @Override
            public String message() {
                return "Invalid UUID format";
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            public Class<? extends Payload>[] payload() {
                @SuppressWarnings("unchecked")
                Class<? extends Payload>[] payload = (Class<? extends Payload>[]) new Class<?>[0];
                return payload;
            }

            @Override
            public boolean allowNull() {
                return false;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return ValidUUID.class;
            }
        };
    }
}
