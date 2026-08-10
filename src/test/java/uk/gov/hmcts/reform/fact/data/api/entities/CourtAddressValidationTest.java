package uk.gov.hmcts.reform.fact.data.api.entities;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CourtAddressValidationTest {

    @Test
    void shouldRejectNullPostcodeAsRequired() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<CourtAddress>> violations =
                validator.validateValue(CourtAddress.class, "postcode", null);

            assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("The postcode must be specified");
        }
    }

    @Test
    void shouldRejectInvalidPostcodeFormat() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<CourtAddress>> violations =
                validator.validateValue(CourtAddress.class, "postcode", "INVALID");

            assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Provided postcode is not valid");
        }
    }

    @Test
    void shouldAcceptValidPostcodeFormat() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(validator.validateValue(CourtAddress.class, "postcode", "SW1A 1AA")).isEmpty();
        }
    }
}

