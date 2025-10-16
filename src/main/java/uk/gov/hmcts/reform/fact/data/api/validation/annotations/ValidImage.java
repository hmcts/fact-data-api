package uk.gov.hmcts.reform.fact.data.api.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import uk.gov.hmcts.reform.fact.data.api.validation.validator.ImageValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ImageValidator.class)
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidImage {
    String message() default "Invalid image file";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String[] allowedTypes() default {"image/jpeg", "image/jpg", "image/png"};
    long maxSize() default 2 * 1024 * 1024; // 2 MB
}

