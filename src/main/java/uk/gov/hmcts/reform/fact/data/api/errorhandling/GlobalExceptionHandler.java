package uk.gov.hmcts.reform.fact.data.api.errorhandling;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.TranslationNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ExceptionResponse handle(NotFoundException ex) {
        log.trace("404, unable to find entity. Details: {}", ex.getMessage());
        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(TranslationNotFoundException.class)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ExceptionResponse handle(TranslationNotFoundException ex) {
        log.trace("204, no translation services for a court. Details: {}", ex.getMessage());
        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(ConstraintViolationException ex) {
        log.error("400, error while validating headers / body. Details: {}", ex.getMessage());

        String message = ex.getConstraintViolations().stream()
            .findFirst()
            .map(v -> v.getConstraintDescriptor().getAnnotation() instanceof ValidUUID
                ? "Invalid UUID supplied: " + v.getInvalidValue()
                : v.getMessage() + ": " + v.getInvalidValue())
            .orElse("Invalid input");

        return generateExceptionResponse(message);
    }

    private ExceptionResponse generateExceptionResponse(String message) {
        ExceptionResponse response = new ExceptionResponse();
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}

