package uk.gov.hmcts.reform.fact.data.api.errorhandling;

import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.AzureUploadException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CsvCreationException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.DuplicatedListItemException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.JsonConvertException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidAreaOfLawException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidDateRangeException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidFileException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidParameterCombinationException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidPostcodeException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;

import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import static uk.gov.hmcts.reform.fact.data.api.utils.LogBuilder.writeLog;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String UNKNOWN = "unknown";
    private static final String ACCESS_DENIED_MESSAGE =
        "Access denied: You do not have permission to access this resource.";
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "An internal server error occurred.";
    private static final String BAD_GATEWAY_MESSAGE =
        "Unable to complete the request due to an upstream service error.";

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ExceptionResponse handle(NotFoundException ex) {
        log.trace(writeLog(
            "404, unable to find entity", ex.getMessage()
        ));
        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ExceptionResponse handle(MultipartException ex, HttpServletRequest request) {
        String provided = request != null && request.getContentType() != null
            ? request.getContentType()
            : UNKNOWN;
        log.warn(writeLog(
            "415, multipart handling error",
            "providedContentType=" + provided,
            ex.getMessage()
        ));

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
        log.trace(writeLog(
            "204, unable to find court resource", ex.getMessage()
        ));
        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(ConstraintViolationException ex) {
        log.warn(writeLog(
            "400, error while validating headers / body", ex.getMessage()
        ));

        String message = ex.getConstraintViolations().stream()
            .findFirst()
            .map(v -> {
                if (v.getConstraintDescriptor().getAnnotation() instanceof ValidUUID) {
                    return "Invalid UUID supplied: " + v.getInvalidValue();
                }

                Object invalidValue = v.getInvalidValue();
                String invalidValueText = invalidValue == null ? "" : invalidValue.toString();

                return invalidValueText.isBlank()
                    ? v.getMessage()
                    : v.getMessage() + ": " + invalidValueText;
            })
            .orElse("Invalid input");

        return generateExceptionResponse(message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handle(MethodArgumentNotValidException ex) {
        log.warn(writeLog(
            "400, error while validating request body", ex.getMessage()
        ));

        LinkedHashMap<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );

        errors.put("timestamp", LocalDateTime.now(ZoneOffset.UTC).toString());
        return errors;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(HttpMessageNotReadableException ex) {
        log.warn(writeLog(
            "400, could not parse request body", ex.getMessage()
        ));

        String message = "Invalid request body: " + ex.getMessage();

        return generateExceptionResponse(message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(IllegalArgumentException ex) {
        log.warn(writeLog(
            "400, illegal argument supplied", ex.getMessage()
        ));
        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(InvalidFileException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(InvalidFileException ex) {
        log.warn(writeLog(
            "400, file failed validation", ex.getMessage()
        ));

        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(InvalidPostcodeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(InvalidPostcodeException ex) {
        log.warn(writeLog(
            "400, invalid postcode", ex.getMessage()
        ));

        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ExceptionResponse handle(MaxUploadSizeExceededException ex) {
        log.warn(writeLog(
            "413, uploaded file size exceeds limit", ex.getMessage()
        ));

        return generateExceptionResponse("Uploaded file size exceeds the maximum allowed limit of 2MB.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(MethodArgumentTypeMismatchException ex) {
        log.warn(writeLog(
            "400, invalid parameter type",
            "Parameter=" + ex.getName(),
            "Value=" + ex.getValue(),
            "ExpectedType=" + (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : UNKNOWN)
        ));

        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : UNKNOWN;
        String message = String.format(
            "Invalid value for parameter '%s': '%s'. Expected type: %s.",
            ex.getName(),
            ex.getValue(),
            expectedType
        );

        return generateExceptionResponse(message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ExceptionResponse handle(AccessDeniedException ex) {
        log.warn(writeLog(
            "403, access denied"
        ));
        return generateExceptionResponse(ACCESS_DENIED_MESSAGE);
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(InvalidDateRangeException ex) {
        log.warn(writeLog(
            "400, date range failed validation", ex.getMessage()
        ));
        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(InvalidParameterCombinationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(InvalidParameterCombinationException ex,
                                    HttpServletRequest request) {
        log.warn(writeLog(
            "400, invalid parameter combination",
            "Path=" + (request != null ? request.getRequestURI() : UNKNOWN),
            ex.getMessage()
        ));
        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(DuplicatedListItemException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(DuplicatedListItemException ex) {
        log.warn(writeLog(
            "400, duplicated list item", ex.getMessage()
        ));
        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(InvalidAreaOfLawException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(InvalidAreaOfLawException ex) {
        log.warn(writeLog(
            "400, invalid area of law", ex.getMessage()
        ));
        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(JsonConvertException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handle(JsonConvertException ex) {
        log.warn(writeLog(
            "400, JSON conversion error", ex.getMessage()
        ));
        return generateExceptionResponse(ex.getMessage());
    }

    @ExceptionHandler(AzureUploadException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ExceptionResponse handle(AzureUploadException ex) {
        log.error(writeLog(
            "502, error while uploading CSV to Azure",
            "ExceptionType=" + ex.getClass().getSimpleName()
        ), ex);
        return generateExceptionResponse(BAD_GATEWAY_MESSAGE);
    }

    @ExceptionHandler(CsvCreationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ExceptionResponse handle(CsvCreationException ex) {
        log.error(writeLog(
            "500, error while creating CSV file",
            "ExceptionType=" + ex.getClass().getSimpleName()
        ), ex);
        return generateExceptionResponse(INTERNAL_SERVER_ERROR_MESSAGE);
    }

    private ExceptionResponse generateExceptionResponse(String message) {
        ExceptionResponse response = new ExceptionResponse();
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now(ZoneOffset.UTC));
        return response;
    }
}

