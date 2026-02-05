package uk.gov.hmcts.reform.fact.data.api.errorhandling;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import jakarta.servlet.http.HttpServletRequest;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.DuplicatedListItemException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidAreaOfLawException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidFileException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidPostcodeException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String UNKNOWN = "unknown";

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ExceptionResponse handle(NotFoundException ex) {
        log.trace("404, unable to find entity. Details: {}", ex.getMessage());
        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ExceptionResponse handle(MultipartException ex, HttpServletRequest request) {
        String provided = request != null && request.getContentType() != null
            ? request.getContentType()
            : UNKNOWN;
        log.error("415, multipart handling error. Provided Content-Type: {}. Details: {}", provided, ex.getMessage());

        String message = String.format(
            "Unsupported or malformed Content-Type '%s'. If uploading a file, use 'multipart/form-data'. "
                + "If sending JSON, use 'application/json'.",
            provided
        );
        return generateExceptionResponse(message);
    }

    @ExceptionHandler(CourtResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ExceptionResponse handle(CourtResourceNotFoundException ex) {
        log.trace("204, unable to find court resource. Details: {}", ex.getMessage());
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handle(MethodArgumentNotValidException ex) {
        log.error("400, error while validating request body. Details: {}", ex.getMessage());

        LinkedHashMap<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );

        errors.put("timestamp", LocalDateTime.now().toString());
        return errors;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(HttpMessageNotReadableException ex) {
        log.error("400, could not parse request body. Details: {}", ex.getMessage());

        String message = "Invalid request body: " + ex.getMessage();

        return generateExceptionResponse(message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(IllegalArgumentException ex) {
        log.error("400, illegal argument supplied. Details: {}", ex.getMessage());
        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(InvalidFileException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(InvalidFileException ex) {
        log.error("400, file failed validation. Details: {}", ex.getMessage());

        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(InvalidPostcodeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(InvalidPostcodeException ex) {
        log.error("400, invalid postcode. Details: {}", ex.getMessage());

        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ExceptionResponse handle(MaxUploadSizeExceededException ex) {
        log.error("413, uploaded file size exceeds limit. Details: {}", ex.getMessage());

        return generateExceptionResponse("Uploaded file size exceeds the maximum allowed limit of 2MB.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(MethodArgumentTypeMismatchException ex) {
        log.error("400, invalid parameter type. Parameter: {}, Value: {}, Expected type: {}",
                  ex.getName(), ex.getValue(), ex.getRequiredType()
                      != null ? ex.getRequiredType().getSimpleName() : UNKNOWN);

        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : UNKNOWN;
        String message = String.format(
            "Invalid value for parameter '%s': '%s'. Expected type: %s.",
            ex.getName(),
            ex.getValue(),
            expectedType
        );

        return generateExceptionResponse(message);
    }

    @ExceptionHandler(DuplicatedListItemException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(DuplicatedListItemException ex) {
        log.error("400, duplicated list item. Details: {}", ex.getMessage());
        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(InvalidAreaOfLawException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(InvalidAreaOfLawException ex) {
        log.error("400, invalid area of law. Details: {}", ex.getMessage());
        return generateExceptionResponse(ex.getMessage());
    }

    private ExceptionResponse generateExceptionResponse(String message) {
        ExceptionResponse response = new ExceptionResponse();
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
