package uk.gov.hmcts.reform.fact.data.api.validator;

import jakarta.validation.ConstraintValidatorContext;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidConditional;
import uk.gov.hmcts.reform.fact.data.api.validation.validator.ConditionalValidator;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(validConditionalAnnotation.requiredAtLeastOne()).thenReturn(new String[]{"value"});
        when(validConditionalAnnotation.message()).thenReturn("Value is required");
        validator.initialize(validConditionalAnnotation);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validScenarios")
    void shouldReturnTrueForValidScenarios(String type, String value, String message) {
        TestObject testObject = new TestObject();
        testObject.setType(type);
        testObject.setValue(value);

        assertTrue(
            validator.isValid(testObject, context),
            message
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

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("multipleRequiredFieldScenarios")
    void shouldRequireAtLeastOneOfMultipleFields(String value, String otherValue, boolean expected, String message) {
        when(validConditionalAnnotation.requiredAtLeastOne()).thenReturn(new String[]{"value", "otherValue"});
        validator.initialize(validConditionalAnnotation);

        TestObject testObject = new TestObject();
        testObject.setType("REQUIRED");
        testObject.setValue(value);
        testObject.setOtherValue(otherValue);

        assertThat(validator.isValid(testObject, context)).as(message).isEqualTo(expected);
    }

    @Test
    void shouldReportViolationAgainstFirstFieldWhenAllMultipleFieldsAreEmpty() {
        when(validConditionalAnnotation.requiredAtLeastOne()).thenReturn(new String[]{"value", "otherValue"});
        validator.initialize(validConditionalAnnotation);

        TestObject testObject = new TestObject();
        testObject.setType("REQUIRED");

        assertFalse(validator.isValid(testObject, context));
        verify(violationBuilder).addPropertyNode("value");
    }

    private static Stream<Arguments> multipleRequiredFieldScenarios() {
        return Stream.of(
            Arguments.of("a", null, true, "Valid when only the first field is populated"),
            Arguments.of(null, "b", true, "Valid when only the second field is populated"),
            Arguments.of("a", "b", true, "Valid when both fields are populated"),
            Arguments.of("", null, false, "Invalid when neither field is populated")
        );
    }

    private static Stream<Arguments> validScenarios() {
        return Stream.of(
            Arguments.of("OTHER", null, "Should be valid when selected value doesn't match condition"),
            Arguments.of("REQUIRED", "some value", "Should be valid when required field is not empty"),
            Arguments.of(null, null, "Should be valid when selected value is null")
        );
    }

    @Getter
    @Setter
    private static class TestObject {
        private String type;
        private String value;
        private String otherValue;
    }
}
