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

@Slf4j
@RestControllerAdvice(assignableTypes = MigrationController.class)
public class MigrationExceptionHandler {

    /**
     * Translates duplicate-execution guards into a 409 response so callers know the run was blocked
     * intentionally.
     *
     * @param ex raised when the migration tables are already populated.
     * @return API-friendly error payload.
     */
    @ExceptionHandler(MigrationAlreadyAppliedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ExceptionResponse handle(MigrationAlreadyAppliedException ex) {
        log.warn("409, migration already applied: {}", ex.getMessage());
        return generateExceptionResponse(ex.getMessage());
    }

    /**
     * Surfaces legacy endpoint failures as 502 errors so the caller can distinguish them from
     * problems inside the new FaCT service.
     *
     * @param ex raised when the HTTP call to the legacy service fails.
     * @return API-friendly error payload.
     */
    @ExceptionHandler(MigrationClientException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(MigrationClientException ex) {
        log.error("400, migration client error: {}", ex.getMessage(), ex);
        return generateExceptionResponse(ex.getMessage());
    }

    /**
     * Shared helper so responses align with the format used elsewhere in the API.
     *
     * @param message description of the failure.
     * @return a populated {@link ExceptionResponse}.
     */
    private ExceptionResponse generateExceptionResponse(String message) {
        ExceptionResponse response = new ExceptionResponse();
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
