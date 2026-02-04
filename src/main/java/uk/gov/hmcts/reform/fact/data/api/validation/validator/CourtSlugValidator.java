package uk.gov.hmcts.reform.fact.data.api.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidCourtSlug;

public class CourtSlugValidator implements ConstraintValidator<ValidCourtSlug, String> {

    /**
     * Validates a slug against configured min/max length and regex
     *
     * @param value slug value to validate
     * @param context validation context
     * @return true when the slug is valid otherwise false
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if (value.isBlank()
            || value.length() < ValidationConstants.COURT_SLUG_MIN_LENGTH
            || value.length() > ValidationConstants.COURT_SLUG_MAX_LENGTH) {
            return fail(context, ValidationConstants.COURT_SLUG_LENGTH_MESSAGE);
        }

        if (!value.matches(ValidationConstants.COURT_SLUG_REGEX)) {
            return fail(context, ValidationConstants.COURT_SLUG_REGEX_MESSAGE);
        }

        return true;
    }

    private static boolean fail(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
