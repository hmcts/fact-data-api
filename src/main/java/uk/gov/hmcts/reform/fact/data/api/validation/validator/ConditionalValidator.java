package uk.gov.hmcts.reform.fact.data.api.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.ObjectUtils;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidConditional;

import java.util.Arrays;

/**
 * A custom constraint validator that enforces conditional validation on
 * an object's properties based on specific conditions.
 * This validator checks if a specified dependent field is required
 * based on the value of a controlling field in the object. When several
 * dependent fields are given, at least one of them must be populated.
 */
public class ConditionalValidator implements ConstraintValidator<ValidConditional, Object> {

    private String selected;
    private String selectedValueForRequired;
    private String[] required;
    private String message;

    @Override
    public void initialize(ValidConditional requiredIfChecked) {
        selected = requiredIfChecked.selected();
        selectedValueForRequired = requiredIfChecked.selectedValueForRequired();
        required = requiredIfChecked.required();
        message = requiredIfChecked.message();
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {
        BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(object);
        Object selectedValue = beanWrapper.getPropertyValue(selected);
        if (selectedValue != null && selectedValue.toString().equals(selectedValueForRequired)) {
            boolean valid = Arrays.stream(required)
                .anyMatch(field -> !ObjectUtils.isEmpty(beanWrapper.getPropertyValue(field)));

            if (!valid) {
                context.disableDefaultConstraintViolation();
                context
                    .buildConstraintViolationWithTemplate(message)
                    .addPropertyNode(required[0])
                    .addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}
