package uk.gov.hmcts.reform.fact.data.api.validator;

import jakarta.validation.ConstraintValidatorContext;
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
        // Set up constraint violation builder chain to avoid NPE during invalid cases
        ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder =
            mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
        when(context.buildConstraintViolationWithTemplate(org.mockito.ArgumentMatchers.anyString())).thenReturn(builder);
        when(builder.addPropertyNode(org.mockito.ArgumentMatchers.anyString())).thenReturn(nodeBuilder);
        when(nodeBuilder.addConstraintViolation()).thenReturn(context);
    }

    static class OpeningHoursBean {
        private LocalTime openingHour;
        private LocalTime closingHour;

        OpeningHoursBean(LocalTime openingHour, LocalTime closingHour) {
            this.openingHour = openingHour;
            this.closingHour = closingHour;
        }

        public LocalTime getOpeningHour() { return openingHour; }
        public LocalTime getClosingHour() { return closingHour; }
    }

    static class NonTimeBean {
        private String openingHour;
        private String closingHour;

        NonTimeBean(String openingHour, String closingHour) {
            this.openingHour = openingHour;
            this.closingHour = closingHour;
        }

        public String getOpeningHour() { return openingHour; }
        public String getClosingHour() { return closingHour; }
    }

    @Test
    void shouldBeValidWhenStartBeforeEnd() {
        OpeningHoursBean bean = new OpeningHoursBean(LocalTime.of(9, 0), LocalTime.of(17, 0));
        assertTrue(validator.isValid(bean, context));
    }

    @Test
    void shouldBeInvalidWhenStartEqualsEnd() {
        prepareContextForViolation();
        OpeningHoursBean bean = new OpeningHoursBean(LocalTime.of(9, 0), LocalTime.of(9, 0));
        assertFalse(validator.isValid(bean, context));
    }

    @Test
    void shouldBeInvalidWhenStartAfterEnd() {
        prepareContextForViolation();
        OpeningHoursBean bean = new OpeningHoursBean(LocalTime.of(18, 0), LocalTime.of(17, 0));
        assertFalse(validator.isValid(bean, context));
    }

    @Test
    void shouldReturnTrueForNullValue() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    void shouldReturnTrueWhenPropertiesAreNotLocalTime() {
        NonTimeBean bean = new NonTimeBean("09:00", "17:00");
        assertTrue(validator.isValid(bean, context));
    }
}
