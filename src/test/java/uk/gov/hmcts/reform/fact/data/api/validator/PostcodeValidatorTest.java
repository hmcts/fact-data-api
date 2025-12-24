package uk.gov.hmcts.reform.fact.data.api.validator;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidPostcode;
import uk.gov.hmcts.reform.fact.data.api.validation.validator.PostcodeValidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostcodeValidatorTest {

    private PostcodeValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @Mock
    private ValidPostcode validPostcodeAnnotation;

    @BeforeEach
    void setUp() {
        validator = new PostcodeValidator();
        validator.initialize(validPostcodeAnnotation);
        lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        lenient().when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SW1A 1AA",
        "EC1A 1BB",
        "W1A 0AX",
        "M1 1AE",
        "B33 8TH",
        "SW1A1AA"
    })
    void shouldAcceptValidPostcodes(String postcode) {
        assertTrue(validator.isValid(postcode, context));
    }

    @Test
    void shouldAcceptPostcodeWithLowercaseAndExtraWhitespace() {
        assertTrue(validator.isValid("  sw1a  1aa  ", context));
    }

    @Test
    void shouldAcceptSpecialCaseGiroPostcode() {
        assertTrue(validator.isValid("GIR 0AA", context));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "NOT A POSTCODE",
        "12345",
        "AA 1AA",
        "AAA 1AA",
        "A1 1A",
        "SW1A 1A@",
        ""
    })
    void shouldRejectInvalidPostcodeFormat(String postcode) {
        assertFalse(validator.isValid(postcode, context));
        verify(context).buildConstraintViolationWithTemplate("Provided postcode is not valid");
    }

    @ParameterizedTest
    @ValueSource(strings = {"EH1 1AA", "G1 1AA", "TD1 1AA"})
    void shouldRejectScotlandPostcodes(String postcode) {
        assertFalse(validator.isValid(postcode, context));
        verify(context).buildConstraintViolationWithTemplate("Scotland is not supported");
    }

    @Test
    void shouldRejectNorthernIrelandPostcodes() {
        assertFalse(validator.isValid("bt11aa", context));
        verify(context).buildConstraintViolationWithTemplate("Northern Ireland is not supported");
    }

    @ParameterizedTest
    @ValueSource(strings = {"IM1 1AA", "JE1 1AA", "GY1 1AA"})
    void shouldRejectChannelIslandsAndIsleOfManPostcodes(String postcode) {
        assertFalse(validator.isValid(postcode, context));
        verify(context).buildConstraintViolationWithTemplate("Postcode region is not supported");
    }
}
