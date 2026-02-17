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
        h.setOpeningTimesDetails(new ArrayList<>(List.of(detail)));
        return h;
    }

    private CourtOpeningHours entry(List<DayOfTheWeek> days) {
        CourtOpeningHours h = new CourtOpeningHours();
        List<OpeningTimesDetail> details = new ArrayList<>();
        for (DayOfTheWeek day : days) {
            OpeningTimesDetail detail = new OpeningTimesDetail();
            detail.setDayOfWeek(day);
            details.add(detail);
        }
        h.setOpeningTimesDetails(details);
        return h;
    }

    @Test
    void shouldReturnFalseForNullList() {
        assertFalse(validator.isValid(null, null));
    }

    @Test
    void shouldReturnFalseForEmptyList() {
        assertFalse(validator.isValid(new CourtOpeningHours(), null));
    }

    @Test
    void shouldReturnTrueWhenOnlyEverydayProvided() {
        CourtOpeningHours openingHours = entry(DayOfTheWeek.EVERYDAY);
        assertTrue(validator.isValid(openingHours, null));
    }

    @Test
    void shouldReturnFalseWhenEverydayWithOtherDays() {
        CourtOpeningHours openingHours = entry(List.of(DayOfTheWeek.EVERYDAY, DayOfTheWeek.MONDAY));
        assertFalse(validator.isValid(openingHours, null));
    }

    @Test
    void shouldReturnFalseWhenDuplicateDaysPresent() {
        CourtOpeningHours openingHours = entry(List.of(
            DayOfTheWeek.MONDAY,
            DayOfTheWeek.TUESDAY,
            DayOfTheWeek.MONDAY
        ));
        assertFalse(validator.isValid(openingHours, null));
    }

    @Test
    void shouldReturnTrueWhenAllDaysUnique() {
        CourtOpeningHours openingHours = entry(List.of(
            DayOfTheWeek.MONDAY,
            DayOfTheWeek.TUESDAY,
            DayOfTheWeek.WEDNESDAY
        ));
        assertTrue(validator.isValid(openingHours, null));
    }
}
