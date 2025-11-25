package uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions;

import lombok.Getter;
import lombok.experimental.StandardException;

@StandardException
@Getter
public class RateLimitExceededException extends RuntimeException {
    private long waitSeconds = -1;

    public RateLimitExceededException(long waitSeconds, String message) {
        super(message);
        this.waitSeconds = waitSeconds;
    }

    public RateLimitExceededException(long waitSeconds, String message, Throwable cause) {
        super(message, cause);
        this.waitSeconds = waitSeconds;
    }

    public RateLimitExceededException(long waitSeconds, Throwable cause) {
        super(cause);
        this.waitSeconds = waitSeconds;
    }
}
