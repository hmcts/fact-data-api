package uk.gov.hmcts.reform.fact.data.api.validator;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;
import uk.gov.hmcts.reform.fact.data.api.validation.validator.UuidValidator;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UuidValidatorTest {

    private UuidValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ValidUUID validUuidAnnotation;

    @BeforeEach
    void setUp() {
        validator = new UuidValidator();
        context = mock(ConstraintValidatorContext.class);
        when(validUuidAnnotation.allowNull()).thenReturn(false);
        validator.initialize(validUuidAnnotation);
    }

    @Test
    void testValidUUID() {
        String validUUID = UUID.randomUUID().toString();
        assertTrue(validator.isValid(validUUID, context),
                   "Validator should return true for a valid UUID");
    }

    @Test
    void testInvalidUUID() {
        String invalidUUID = "not-a-uuid";
        assertFalse(validator.isValid(invalidUUID, context),
                    "Validator should return false for an invalid UUID");
    }

    @Test
    void testNullUuid() {
        assertFalse(validator.isValid(null, context),
                    "Validator should return false for null value");
    }

    @Test
    void testEmptyUuid() {
        assertFalse(validator.isValid("", context),
                    "Validator should return false for empty string");
    }

    @Test
    void testNullUuidAllowedWhenConfigured() {
        ValidUUID annotationAllowingNull = mock(ValidUUID.class);
        when(annotationAllowingNull.allowNull()).thenReturn(true);

        UuidValidator validatorAllowingNull = new UuidValidator();
        validatorAllowingNull.initialize(annotationAllowingNull);

        assertTrue(validatorAllowingNull.isValid(null, context),
                   "Validator should return true for null when allowNull is true");
    }

    @Test
    void testBlankUuidAllowedWhenConfigured() {
        ValidUUID annotationAllowingNull = mock(ValidUUID.class);
        when(annotationAllowingNull.allowNull()).thenReturn(true);

        UuidValidator validatorAllowingNull = new UuidValidator();
        validatorAllowingNull.initialize(annotationAllowingNull);

        assertTrue(validatorAllowingNull.isValid("   ", context),
                   "Validator should return true for blank value when allowNull is true");
    }
}
