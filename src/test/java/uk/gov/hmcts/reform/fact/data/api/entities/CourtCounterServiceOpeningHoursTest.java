package uk.gov.hmcts.reform.fact.data.api.entities;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.reform.fact.data.api.entities.types.DayOfTheWeek;
import uk.gov.hmcts.reform.fact.data.api.entities.types.OpeningTimesDetail;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CourtCounterServiceOpeningHoursTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    void getCourtTypesForViewReturnsExpandedCourtTypesWhenPresent() {
        CourtType courtType = CourtType.builder().id(UUID.randomUUID()).name("County Court").build();
        CourtCounterServiceOpeningHours openingHours = new CourtCounterServiceOpeningHours();
        openingHours.setCourtTypeDetails(List.of(courtType));

        assertThat(openingHours.getCourtTypesForView()).containsExactly(courtType);
    }

    @Test
    void getCourtTypesForViewFallsBackToCourtTypeIds() {
        UUID courtTypeId = UUID.randomUUID();
        CourtCounterServiceOpeningHours openingHours = new CourtCounterServiceOpeningHours();
        openingHours.setCourtTypes(List.of(courtTypeId));

        assertThat(openingHours.getCourtTypesForView()).containsExactly(courtTypeId);
    }

    @Test
    void getCourtTypesForViewReturnsNullWhenNeitherDetailsNorIdsAreSet() {
        CourtCounterServiceOpeningHours openingHours = new CourtCounterServiceOpeningHours();

        assertThat(openingHours.getCourtTypesForView()).isNull();
    }

    @Test
    void appointmentEmailIsSerialisedAsAppointmentContactToKeepApiContract() {
        CourtCounterServiceOpeningHours openingHours = CourtCounterServiceOpeningHours.builder()
            .appointmentEmail("book@court.gov.uk")
            .appointmentPhone("01234 567890")
            .build();

        String json = MAPPER.writeValueAsString(openingHours);

        assertThat(json)
            .contains("\"appointmentContact\":\"book@court.gov.uk\"")
            .contains("\"appointmentPhone\":\"01234 567890\"")
            .doesNotContain("appointmentEmail");
    }

    @Test
    void appointmentContactIsDeserialisedIntoAppointmentEmail() {
        CourtCounterServiceOpeningHours openingHours = MAPPER.readValue(
            "{\"appointmentContact\":\"book@court.gov.uk\",\"appointmentPhone\":\"01234 567890\"}",
            CourtCounterServiceOpeningHours.class
        );

        assertThat(openingHours.getAppointmentEmail()).isEqualTo("book@court.gov.uk");
        assertThat(openingHours.getAppointmentPhone()).isEqualTo("01234 567890");
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("appointmentContactScenarios")
    void validatesAppointmentContacts(Boolean appointmentNeeded, String email, String phone, String expectedMessage) {
        CourtCounterServiceOpeningHours openingHours = CourtCounterServiceOpeningHours.builder()
            .courtId(UUID.randomUUID())
            .counterService(true)
            .assistWithForms(true)
            .assistWithDocuments(false)
            .assistWithSupport(false)
            .appointmentNeeded(appointmentNeeded)
            .appointmentEmail(email)
            .appointmentPhone(phone)
            .openingTimesDetails(List.of(
                OpeningTimesDetail.builder()
                    .dayOfWeek(DayOfTheWeek.MONDAY)
                    .openingTime(LocalTime.of(9, 0))
                    .closingTime(LocalTime.of(17, 0))
                    .build()
            ))
            .build();

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<CourtCounterServiceOpeningHours>> violations = validator.validate(openingHours);

            if (expectedMessage == null) {
                assertThat(violations).isEmpty();
            } else {
                assertThat(violations).extracting(ConstraintViolation::getMessage).containsExactly(expectedMessage);
            }
        }
    }

    private static Stream<Arguments> appointmentContactScenarios() {
        return Stream.of(
            Arguments.of(true, "book@court.gov.uk", null, null),
            Arguments.of(true, null, "01234 567890", null),
            Arguments.of(true, "book@court.gov.uk", "+44 1234 567890", null),
            Arguments.of(false, null, null, null),
            Arguments.of(true, null, null, ValidationConstants.APPOINTMENT_CONTACT_REQUIRED_MESSAGE),
            Arguments.of(true, "", "", ValidationConstants.APPOINTMENT_CONTACT_REQUIRED_MESSAGE),
            Arguments.of(true, null, "not-a-phone", ValidationConstants.PHONE_NO_REGEX_MESSAGE),
            Arguments.of(true, "not-an-email", null, ValidationConstants.EMAIL_REGEX_MESSAGE)
        );
    }
}
