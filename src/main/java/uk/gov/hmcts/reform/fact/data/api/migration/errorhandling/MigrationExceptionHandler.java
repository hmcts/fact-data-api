package uk.gov.hmcts.reform.fact.data.api.migration.errorhandling;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.ExceptionResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.controller.MigrationController;
import uk.gov.hmcts.reform.fact.data.api.migration.exception.MigrationAlreadyAppliedException;
import uk.gov.hmcts.reform.fact.data.api.migration.exception.MigrationClientException;

/**
 * Scoped exception handler for the private migration endpoint so the migration module can be removed
 * cleanly once the feature is decommissioned.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = MigrationController.class)
public class MigrationExceptionHandler {

    @ExceptionHandler(MigrationAlreadyAppliedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ExceptionResponse handle(MigrationAlreadyAppliedException ex) {
        log.warn("409, migration already applied: {}", ex.getMessage());
        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(MigrationClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ExceptionResponse handle(MigrationClientException ex) {
        log.error("502, migration client error: {}", ex.getMessage(), ex);
        return generateExceptionResponse(ex.getMessage());
    }

    private ExceptionResponse generateExceptionResponse(String message) {
        ExceptionResponse response = new ExceptionResponse();
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
