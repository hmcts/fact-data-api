package uk.gov.hmcts.reform.fact.data.api.entities;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ServiceCentreAddressValidationTest {

    @Test
    void shouldRejectNullPostcodeAsRequired() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<ServiceCentreAddress>> violations =
                validator.validateValue(ServiceCentreAddress.class, "postcode", null);

            assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("The postcode must be specified");
        }
    }

    @Test
    void shouldAcceptValidPostcodeFormat() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<ServiceCentreAddress>> violations =
                validator.validateValue(ServiceCentreAddress.class, "postcode", "SW1A 1AA");

            assertThat(violations).isEmpty();
        }
    }
}

