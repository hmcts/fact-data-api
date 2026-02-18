package uk.gov.hmcts.reform.fact.data.api.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.types.DayOfTheWeek;

import uk.gov.hmcts.reform.fact.data.api.entities.types.OpeningTimesDetail;
import uk.gov.hmcts.reform.fact.data.api.validation.validator.UniqueOpeningDaysValidator;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniqueOpeningDaysForCounterServiceValidatorTest {

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
        List<OpeningTimesDetail> list = List.of(entry(DayOfTheWeek.EVERYDAY));
        assertTrue(validator.isValid(list, null));
    }

    @Test
    void shouldReturnFalseWhenEverydayWithOtherDays() {
        List<OpeningTimesDetail> list = List.of(
            entry(DayOfTheWeek.EVERYDAY),
            entry(DayOfTheWeek.MONDAY)
        );
        assertFalse(validator.isValid(list, null));
    }

    @Test
    void shouldReturnFalseWhenDuplicateDaysPresent() {
        List<OpeningTimesDetail> list = List.of(
            entry(DayOfTheWeek.MONDAY),
            entry(DayOfTheWeek.TUESDAY),
            entry(DayOfTheWeek.MONDAY)
        );
        assertFalse(validator.isValid(list, null));
    }

    @Test
    void shouldReturnTrueWhenAllDaysUnique() {
        List<OpeningTimesDetail> list = List.of(
            entry(DayOfTheWeek.MONDAY),
            entry(DayOfTheWeek.TUESDAY),
            entry(DayOfTheWeek.WEDNESDAY)
        );
        assertTrue(validator.isValid(list, null));
    }

    @Test
    void shouldIgnoreNullEntriesAndNullDays() {
        OpeningTimesDetail nullDay = new OpeningTimesDetail();
        nullDay.setDayOfWeek(null);

        List<OpeningTimesDetail> list = new ArrayList<>();
        list.add(null); // null entry
        list.add(entry(DayOfTheWeek.THURSDAY));
        list.add(nullDay); // entry with null day
        list.add(entry(DayOfTheWeek.FRIDAY));

        assertTrue(validator.isValid(list, null));
    }
}
