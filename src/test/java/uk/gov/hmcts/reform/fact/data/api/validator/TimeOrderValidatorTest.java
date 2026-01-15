package uk.gov.hmcts.reform.fact.data.api.validator;

import jakarta.validation.ConstraintValidatorContext;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidTimeOrder;
import uk.gov.hmcts.reform.fact.data.api.validation.validator.TimeOrderValidator;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeOrderValidatorTest {

    private TimeOrderValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ValidTimeOrder annotation;

    @BeforeEach
    void setUp() {
        validator = new TimeOrderValidator();
        context = mock(ConstraintValidatorContext.class);
        when(annotation.start()).thenReturn("openingHour");
        when(annotation.end()).thenReturn("closingHour");
        when(annotation.message()).thenReturn("End time must be after start time");
        validator.initialize(annotation);
    }

    private void prepareContextForViolation() {

        ConstraintValidatorContext.ConstraintViolationBuilder builder
            = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder =
            mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
        when(
            context.buildConstraintViolationWithTemplate(org.mockito.ArgumentMatchers.anyString())).thenReturn(builder);
        when(builder.addPropertyNode(org.mockito.ArgumentMatchers.anyString())).thenReturn(nodeBuilder);
        when(nodeBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    void shouldBeValidWhenStartBeforeEnd() {
        TestObject testObject = new TestObject(LocalTime.of(9, 0), LocalTime.of(17, 0));
        assertTrue(validator.isValid(testObject, context));
    }

    @Test
    void shouldBeInvalidWhenStartEqualsEnd() {
        prepareContextForViolation();
        TestObject testObject = new TestObject(LocalTime.of(9, 0), LocalTime.of(9, 0));
        assertFalse(validator.isValid(testObject, context));
    }

    @Test
    void shouldBeInvalidWhenStartAfterEnd() {
        prepareContextForViolation();
        TestObject testObject = new TestObject(LocalTime.of(18, 0), LocalTime.of(17, 0));
        assertFalse(validator.isValid(testObject, context));
    }

    @Test
    void shouldReturnTrueForNullValue() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    void shouldReturnTrueWhenPropertiesAreNotLocalTime() {
        NonTimeTestObject testObject = new NonTimeTestObject("09:00", "17:00");
        assertTrue(validator.isValid(testObject, context));
    }

    @Getter
    @Setter
    private static class TestObject {
        private LocalTime openingHour;
        private LocalTime closingHour;

        TestObject(LocalTime openingHour, LocalTime closingHour) {
            this.openingHour = openingHour;
            this.closingHour = closingHour;
        }
    }

    @Getter
    @Setter
    private static class NonTimeTestObject {
        private String openingHour;
        private String closingHour;

        NonTimeTestObject(String openingHour, String closingHour) {
            this.openingHour = openingHour;
            this.closingHour = closingHour;
        }
    }
}
