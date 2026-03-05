package uk.gov.hmcts.reform.fact.data.api.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.types.DayOfTheWeek;
import uk.gov.hmcts.reform.fact.data.api.entities.types.OpeningTimesDetail;
import uk.gov.hmcts.reform.fact.data.api.validation.validator.UniqueOpeningDaysValidator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniqueOpeningDaysValidatorTest {

    private UniqueOpeningDaysValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UniqueOpeningDaysValidator();
    }

    private OpeningTimesDetail entry(DayOfTheWeek day) {
        OpeningTimesDetail detail = new OpeningTimesDetail();
        detail.setDayOfWeek(day);
        return detail;
    }

    @Test
    void shouldReturnFalseForNullList() {
        assertFalse(validator.isValid(null, null));
    }

    @Test
    void shouldReturnFalseForEmptyList() {
        assertFalse(validator.isValid(List.of(), null));
    }

    @Test
    void shouldReturnTrueWhenOnlyEverydayProvided() {
        assertTrue(validator.isValid(List.of(entry(DayOfTheWeek.EVERYDAY)), null));
    }

    @Test
    void shouldReturnFalseWhenEverydayWithOtherDays() {
        assertFalse(validator.isValid(List.of(entry(DayOfTheWeek.EVERYDAY), entry(DayOfTheWeek.MONDAY)), null));
    }

    @Test
    void shouldReturnFalseWhenDuplicateDaysPresent() {
        assertFalse(validator.isValid(List.of(
            entry(DayOfTheWeek.MONDAY),
            entry(DayOfTheWeek.TUESDAY),
            entry(DayOfTheWeek.MONDAY)
        ), null));
    }

    @Test
    void shouldReturnTrueWhenAllDaysUnique() {
        assertTrue(validator.isValid(List.of(
            entry(DayOfTheWeek.MONDAY),
            entry(DayOfTheWeek.TUESDAY),
            entry(DayOfTheWeek.WEDNESDAY)
        ), null));
    }
}
