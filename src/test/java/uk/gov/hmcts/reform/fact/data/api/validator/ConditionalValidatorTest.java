package uk.gov.hmcts.reform.fact.data.api.validator;

import jakarta.validation.ConstraintValidatorContext;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidConditional;
import uk.gov.hmcts.reform.fact.data.api.validation.validator.ConditionalValidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConditionalValidatorTest {

    private ConditionalValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    @Mock
    private ValidConditional validConditionalAnnotation;

    @BeforeEach
    void setUp() {
        validator = new ConditionalValidator();
        lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        lenient().when(violationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
        when(validConditionalAnnotation.selected()).thenReturn("type");
        when(validConditionalAnnotation.selectedValueForRequired()).thenReturn("REQUIRED");
        when(validConditionalAnnotation.required()).thenReturn("value");
        when(validConditionalAnnotation.message()).thenReturn("Value is required");
        validator.initialize(validConditionalAnnotation);
    }

    @Test
    void shouldReturnTrueWhenSelectedValueDoesNotMatchCondition() {
        TestObject testObject = new TestObject();
        testObject.setType("OTHER");
        testObject.setValue(null);

        assertTrue(
            validator.isValid(testObject, context),
            "Should be valid when selected value doesn't match condition"
        );
    }

    @Test
    void shouldReturnTrueWhenSelectedValueMatchesAndRequiredFieldIsNotEmpty() {
        TestObject testObject = new TestObject();
        testObject.setType("REQUIRED");
        testObject.setValue("some value");

        assertTrue(
            validator.isValid(testObject, context),
            "Should be valid when required field is not empty"
        );
    }

    @Test
    void shouldReturnFalseWhenSelectedValueMatchesAndRequiredFieldIsEmpty() {
        TestObject testObject = new TestObject();
        testObject.setType("REQUIRED");
        testObject.setValue("");

        assertFalse(
            validator.isValid(testObject, context),
            "Should be invalid when required field is empty"
        );
        verify(context).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldReturnFalseWhenSelectedValueMatchesAndRequiredFieldIsNull() {
        TestObject testObject = new TestObject();
        testObject.setType("REQUIRED");
        testObject.setValue(null);

        assertFalse(
            validator.isValid(testObject, context),
            "Should be invalid when required field is null"
        );
        verify(context).buildConstraintViolationWithTemplate(anyString());
    }

    @Getter
    @Setter
    private static class TestObject {
        private String type;
        private String value;
    }
}
