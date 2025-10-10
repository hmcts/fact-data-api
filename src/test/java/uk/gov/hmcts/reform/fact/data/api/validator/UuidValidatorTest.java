package uk.gov.hmcts.reform.fact.data.api.validator;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.validation.validator.UuidValidator;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class UuidValidatorTest {

    private UuidValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new UuidValidator();
        context = mock(ConstraintValidatorContext.class);
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
    void testNullUUID() {
        assertFalse(validator.isValid(null, context),
                    "Validator should return false for null value");
    }

    @Test
    void testEmptyUUID() {
        assertFalse(validator.isValid("", context),
                    "Validator should return false for empty string");
    }
}

