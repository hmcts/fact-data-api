package uk.gov.hmcts.reform.fact.data.api.errorhandling;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;

import java.util.Collections;
import java.util.Map;

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
    void testHandleCourtResourceNotFoundException() {
        CourtResourceNotFoundException ex = new CourtResourceNotFoundException(TEST_MESSAGE);
        ExceptionResponse response = handler.handle(ex);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo(TEST_MESSAGE);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void testHandleConstraintViolationException() {
        ConstraintViolationException ex = new ConstraintViolationException(TEST_MESSAGE, Collections.emptySet());
        ExceptionResponse response = handler.handle(ex);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Invalid input");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void testHandleMethodArgumentNotValidException() {
        FieldError fieldError = new FieldError("object", "field", TEST_MESSAGE);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "object");
        bindingResult.addError(fieldError);

        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);

        Map<String, String> response = handler.handle(methodArgumentNotValidException);

        assertThat(response).isNotNull();
        assertThat(response).containsEntry("field", TEST_MESSAGE);
        assertThat(response).containsKey("timestamp");
    }

    @Test
    void testHandleHttpMessageNotReadableException() {
        String exceptionMessage = "Cannot deserialize value of type `java.util.UUID` from String \"hello\"";
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn(exceptionMessage);

        ExceptionResponse response = handler.handle(ex);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("Invalid request body");
        assertThat(response.getMessage()).contains(exceptionMessage);
        assertThat(response.getTimestamp()).isNotNull();
    }

}
