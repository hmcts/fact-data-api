package uk.gov.hmcts.reform.fact.data.api.errorhandling;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExceptionResponse {
    private  String message;
    private LocalDateTime timestamp;
}
