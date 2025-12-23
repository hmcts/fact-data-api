package uk.gov.hmcts.reform.fact.data.api.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidPostcode;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.UUID;

public class PostcodeValidator implements ConstraintValidator<ValidPostcode, String> {

    @Override
    public void initialize(ValidPostcode constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        try {
            System.out.println("goes in here");
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
