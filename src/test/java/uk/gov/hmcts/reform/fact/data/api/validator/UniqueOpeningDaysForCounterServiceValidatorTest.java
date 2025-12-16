package uk.gov.hmcts.reform.fact.data.api.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.types.DayOfTheWeek;
import uk.gov.hmcts.reform.fact.data.api.validation.validator.UniqueOpeningDaysForCounterServiceValidator;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniqueOpeningDaysForCounterServiceValidatorTest {

    private UniqueOpeningDaysForCounterServiceValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UniqueOpeningDaysForCounterServiceValidator();
    }

    private CourtCounterServiceOpeningHours entry(DayOfTheWeek day) {
        CourtCounterServiceOpeningHours h = new CourtCounterServiceOpeningHours();
        h.setDayOfWeek(day);
        return h;
    }

    @Test
    void shouldReturnFalseForNullList() {
        assertFalse(validator.isValid(null, null));
    }

    @Test
    void shouldReturnFalseForEmptyList() {
        assertFalse(validator.isValid(new ArrayList<>(), null));
    }

    @Test
    void shouldReturnTrueWhenOnlyEverydayProvided() {
        List<CourtCounterServiceOpeningHours> list = List.of(entry(DayOfTheWeek.EVERYDAY));
        assertTrue(validator.isValid(list, null));
    }

    @Test
    void shouldReturnFalseWhenEverydayWithOtherDays() {
        List<CourtCounterServiceOpeningHours> list = List.of(
            entry(DayOfTheWeek.EVERYDAY),
            entry(DayOfTheWeek.MONDAY)
        );
        assertFalse(validator.isValid(list, null));
    }

    @Test
    void shouldReturnFalseWhenDuplicateDaysPresent() {
        List<CourtCounterServiceOpeningHours> list = List.of(
            entry(DayOfTheWeek.MONDAY),
            entry(DayOfTheWeek.TUESDAY),
            entry(DayOfTheWeek.MONDAY)
        );
        assertFalse(validator.isValid(list, null));
    }

    @Test
    void shouldReturnTrueWhenAllDaysUnique() {
        List<CourtCounterServiceOpeningHours> list = List.of(
            entry(DayOfTheWeek.MONDAY),
            entry(DayOfTheWeek.TUESDAY),
            entry(DayOfTheWeek.WEDNESDAY)
        );
        assertTrue(validator.isValid(list, null));
    }

    @Test
    void shouldIgnoreNullEntriesAndNullDays() {
        CourtCounterServiceOpeningHours nullDay = new CourtCounterServiceOpeningHours();
        nullDay.setDayOfWeek(null);

        List<CourtCounterServiceOpeningHours> list = new ArrayList<>();
        list.add(null); // null entry
        list.add(entry(DayOfTheWeek.THURSDAY));
        list.add(nullDay); // entry with null day
        list.add(entry(DayOfTheWeek.FRIDAY));

        assertTrue(validator.isValid(list, null));
    }
}
