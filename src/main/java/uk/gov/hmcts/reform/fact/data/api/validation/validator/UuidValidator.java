package uk.gov.hmcts.reform.fact.data.api.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.UUID;

public class UuidValidator implements ConstraintValidator<ValidUUID, String> {

    private boolean allowNull;

    @Override
    public void initialize(ValidUUID constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return allowNull;
        }

        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
