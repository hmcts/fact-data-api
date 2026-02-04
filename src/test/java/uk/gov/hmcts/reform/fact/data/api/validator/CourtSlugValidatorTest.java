package uk.gov.hmcts.reform.fact.data.api.validator;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.validation.validator.CourtSlugValidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourtSlugValidatorTest {

    private CourtSlugValidator validator;

    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new CourtSlugValidator();
        context = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder =
            mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    void testValidCourtSlug() {
        assertTrue(validator.isValid("test-court-123", context));
    }

    @Test
    void testSlugAtMinLengthIsValid() {
        assertTrue(validator.isValid("abcde", context));
    }

    @Test
    void testSlugAtMaxLengthIsValid() {
        assertTrue(validator.isValid("a".repeat(250), context));
    }

    @Test
    void testInvalidCourtSlugWithUppercaseCharacters() {
        assertFalse(validator.isValid("Test-Court-123", context));
    }

    @Test
    void testInvalidCourtSlugWithApostrophe() {
        assertFalse(validator.isValid("court's-123", context));
    }

    @Test
    void testInvalidCourtSlugWithBrackets() {
        assertFalse(validator.isValid("court(family)-123", context));
    }

    @Test
    void testInvalidCourtSlugWithSymbols() {
        assertFalse(validator.isValid("test/court", context));
    }

    @Test
    void testInvalidCourtSlugWithAmpersand() {
        assertFalse(validator.isValid("Court&123", context));
    }

    @Test
    void testNullSlugAllowed() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    void testBlankSlugNotAllowed() {
        assertFalse(validator.isValid("   ", context));
    }

    @Test
    void testSlugTooShort() {
        assertFalse(validator.isValid("abcd", context));
    }

    @Test
    void testSlugTooLong() {
        assertFalse(validator.isValid("a".repeat(251), context));
    }
}
