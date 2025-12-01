package uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions;

import lombok.experimental.StandardException;

@StandardException
public class OsProcessException extends RuntimeException {
    public OsProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}
