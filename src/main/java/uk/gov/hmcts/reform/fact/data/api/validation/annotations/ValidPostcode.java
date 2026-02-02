package uk.gov.hmcts.reform.fact.data.api.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import uk.gov.hmcts.reform.fact.data.api.validation.validator.PostcodeValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates a postcode that is supported for OS address lookup.
 */
@Documented
@Constraint(validatedBy = PostcodeValidator.class)
@Target({ ElementType.PARAMETER })@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPostcode {
    /**
     * Returns the default validation error message.
     *
     * @return the default message
     */
    String message() default "Invalid postcode format";

    /**
     * Returns the validation groups for this constraint.
     *
     * @return the validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Returns the payload for clients of the Bean Validation API.
     *
     * @return the payload
     */
    Class<? extends Payload>[] payload() default {};
}
