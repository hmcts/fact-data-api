package uk.gov.hmcts.reform.fact.data.api.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.types.DayOfTheWeek;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.UniqueOpeningDays;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Validator to ensure court counter service opening hours have unique days of the week.
 * This validator implements the following rules:
 * - If the list contains EVERYDAY, it must be the only entry
 * - Each day of the week can only appear once in the list
 * - Null values in the list or null days are ignored
 * - Empty or null lists are considered invalid
 */
public class UniqueOpeningDaysForCounterServiceValidator implements ConstraintValidator<UniqueOpeningDays, List<CourtCounterServiceOpeningHours>> {

    @Override
    public boolean isValid(List<CourtCounterServiceOpeningHours> value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        boolean containsEveryday = value.stream()
            .filter(java.util.Objects::nonNull)
            .map(CourtCounterServiceOpeningHours::getDayOfWeek)
            .anyMatch(day -> day == DayOfTheWeek.EVERYDAY);

        if (containsEveryday) {
            return value.size() == 1;
        }

        Set<DayOfTheWeek> seen = new HashSet<>();
        for (CourtCounterServiceOpeningHours h : value) {
            if (h == null) {
                continue;
            }
            DayOfTheWeek day = h.getDayOfWeek();
            if (day == null) {
                continue;
            }
            if (!seen.add(day)) {
                return false;
            }
        }
        return true;
    }
}
