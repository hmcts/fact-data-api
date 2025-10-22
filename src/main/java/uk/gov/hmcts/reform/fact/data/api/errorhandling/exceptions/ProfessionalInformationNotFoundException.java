package uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions;

public class ProfessionalInformationNotFoundException extends RuntimeException {

    public ProfessionalInformationNotFoundException(String message) {
        super(message);
    }
}
