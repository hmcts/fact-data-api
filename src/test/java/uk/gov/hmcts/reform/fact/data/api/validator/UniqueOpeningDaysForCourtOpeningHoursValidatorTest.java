package uk.gov.hmcts.reform.fact.data.api.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.types.DayOfTheWeek;
import uk.gov.hmcts.reform.fact.data.api.entities.types.OpeningTimesDetail;
import uk.gov.hmcts.reform.fact.data.api.validation.validator.UniqueOpeningDaysForCourtOpeningHoursValidator;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniqueOpeningDaysForCourtOpeningHoursValidatorTest {

    private UniqueOpeningDaysForCourtOpeningHoursValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UniqueOpeningDaysForCourtOpeningHoursValidator();
    }

    private CourtOpeningHours entry(DayOfTheWeek day) {
        CourtOpeningHours h = new CourtOpeningHours();
        OpeningTimesDetail detail = new OpeningTimesDetail();
        detail.setDayOfWeek(day);
        h.setOpeningTimesDetails(new ArrayList<OpeningTimesDetail>() {
            {
                add(detail);
            }
        });
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
        List<CourtOpeningHours> list = List.of(entry(DayOfTheWeek.EVERYDAY));
        assertTrue(validator.isValid(list, null));
    }

    @Test
    void shouldReturnFalseWhenEverydayWithOtherDays() {
        List<CourtOpeningHours> list = List.of(
            entry(DayOfTheWeek.EVERYDAY),
            entry(DayOfTheWeek.MONDAY)
        );
        assertFalse(validator.isValid(list, null));
    }

    @Test
    void shouldReturnFalseWhenDuplicateDaysPresent() {
        List<CourtOpeningHours> list = List.of(
            entry(DayOfTheWeek.MONDAY),
            entry(DayOfTheWeek.TUESDAY),
            entry(DayOfTheWeek.MONDAY)
        );
        assertFalse(validator.isValid(list, null));
    }

    @Test
    void shouldReturnTrueWhenAllDaysUnique() {
        List<CourtOpeningHours> list = List.of(
            entry(DayOfTheWeek.MONDAY),
            entry(DayOfTheWeek.TUESDAY),
            entry(DayOfTheWeek.WEDNESDAY)
        );
        assertTrue(validator.isValid(list, null));
    }
}
