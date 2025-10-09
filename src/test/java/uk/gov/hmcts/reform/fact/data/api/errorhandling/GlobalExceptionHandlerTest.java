package uk.gov.hmcts.reform.fact.data.api.errorhandling;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.TranslationNotFoundException;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
        assertThat(response).containsKey("field");
        assertThat(response.get("field")).isEqualTo(TEST_MESSAGE);
    }
}
