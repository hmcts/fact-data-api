package uk.gov.hmcts.reform.fact.data.api.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import uk.gov.hmcts.reform.fact.data.api.validation.validator.UniqueOpeningDaysValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = {
    UniqueOpeningDaysValidator.class,
})
@Target({ PARAMETER, FIELD, TYPE_USE })
@Retention(RUNTIME)
public @interface UniqueOpeningDays {
    String message() default
        "Opening hours require at least one entry. "
            + "Requests must be unique for each day of the week. "
            + "Requests for EVERYDAY must be the only entry.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
