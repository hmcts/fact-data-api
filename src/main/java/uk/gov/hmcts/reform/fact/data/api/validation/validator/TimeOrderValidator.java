package uk.gov.hmcts.reform.fact.data.api.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidTimeOrder;

import java.time.LocalTime;

/**
 * Validator to ensure a start time is strictly before an end time.
 */
public class TimeOrderValidator implements ConstraintValidator<ValidTimeOrder, Object> {

    private String startField;
    private String endField;
    private String message;

    @Override
    public void initialize(ValidTimeOrder annotation) {
        this.startField = annotation.start();
        this.endField = annotation.end();
        this.message = annotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(value);
        Object startObj = beanWrapper.getPropertyValue(startField);
        Object endObj = beanWrapper.getPropertyValue(endField);

        if (!(startObj instanceof LocalTime) || !(endObj instanceof LocalTime)) {
            return true;
        }

        LocalTime start = (LocalTime) startObj;
        LocalTime end = (LocalTime) endObj;

        boolean valid = start.isBefore(end);
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context
                .buildConstraintViolationWithTemplate(message)
                .addPropertyNode(endField)
                .addConstraintViolation();
        }
        return valid;
    }
}
