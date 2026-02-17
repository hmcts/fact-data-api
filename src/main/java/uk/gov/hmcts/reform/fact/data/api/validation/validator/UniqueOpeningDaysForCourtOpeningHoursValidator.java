package uk.gov.hmcts.reform.fact.data.api.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.types.DayOfTheWeek;
import uk.gov.hmcts.reform.fact.data.api.entities.types.OpeningTimesDetail;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.UniqueOpeningDays;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validator to ensure court opening hours have unique days of the week.
 * This validator implements the following rules:
 * - If the list contains EVERYDAY, it must be the only entry
 * - Each day of the week can only appear once in the list
 * - Null values in the list or null days are ignored
 * - Empty or null lists are considered invalid
 */
public class UniqueOpeningDaysForCourtOpeningHoursValidator
    implements ConstraintValidator<UniqueOpeningDays, CourtOpeningHours> {

    @Override
    public boolean isValid(CourtOpeningHours value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        List<OpeningTimesDetail> details = value.getOpeningTimesDetails();
        if (details == null || details.isEmpty()) {
            return false;
        }

        boolean containsEveryday = details.stream()
            .filter(d -> d != null && d.getDayOfWeek() != null)
            .anyMatch(d -> d.getDayOfWeek() == DayOfTheWeek.EVERYDAY);

        if (containsEveryday) {
            return details.size() == 1;
        }

        Set<DayOfTheWeek> seen = new HashSet<>();
        for (OpeningTimesDetail detail : details) {
            if (detail == null || detail.getDayOfWeek() == null) {
                continue;
            }

            DayOfTheWeek day = detail.getDayOfWeek();
            if (!seen.add(day)) {
                return false;
            }
        }

        return true;
    }
}
