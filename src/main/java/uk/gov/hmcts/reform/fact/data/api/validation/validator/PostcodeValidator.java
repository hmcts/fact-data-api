package uk.gov.hmcts.reform.fact.data.api.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidPostcode;

import java.util.Locale;
import java.util.regex.Pattern;

public class PostcodeValidator implements ConstraintValidator<ValidPostcode, String> {

    private static final Pattern UK_POSTCODE_REGEX = Pattern.compile(
        "([Gg][Ii][Rr] 0[Aa]{2})|((([A-Za-z]\\d{1,2})|(([A-Za-z]"
            + "[A-Ha-hJ-Yj-y]\\d{1,2})|(([A-Za-z]\\d[A-Za-z])|([A-Za-z][A-Ha-hJ-Yj-y]"
            + "\\d[A-Za-z]?))))\\s?\\d[A-Za-z]{2})"
    );
    private static final Pattern SCOTLAND_AREA_REGEX = Pattern.compile(
        "^(ZE|KW|IV|HS|PH|AB|DD|PA|FK|G\\d|KY|KA|DG|EH|ML|TD)",
        Pattern.CASE_INSENSITIVE
    );
    private static final String NI_PREFIX = "BT";
    private static final Pattern CI_IOM_AREA_REGEX = Pattern.compile("^(IM|JE|GY)", Pattern.CASE_INSENSITIVE);

    /**
     * Initializes the validator.
     *
     * @param constraintAnnotation the annotation instance
     */
    @Override
    public void initialize(ValidPostcode constraintAnnotation) {
    }

    /**
     * Validates a postcode against supported formats and regions.
     *
     * @param rawPostcode the raw input postcode
     * @param context the validation context
     * @return true if valid and supported
     */
    @Override
    public boolean isValid(String rawPostcode, ConstraintValidatorContext context) {
        String postcode = normalize(rawPostcode);

        if (!UK_POSTCODE_REGEX.matcher(postcode).matches()) {
            return fail(context, "Provided postcode is not valid");
        }

        if (SCOTLAND_AREA_REGEX.matcher(postcode).find()) {
            return fail(context, "Scotland is not supported");
        }

        if (postcode.startsWith(NI_PREFIX)) {
            return fail(context, "Northern Ireland is not supported");
        }

        if (CI_IOM_AREA_REGEX.matcher(postcode).find()) {
            return fail(context, "Postcode region is not supported");
        }

        if (!postcode.contains(" ")) {
            return fail(context, "Postcode must contain "
                + "a space between inward and outward codes for OS lookup");
        }

        return true;
    }

    /**
     * Adds a custom validation failure and returns false.
     *
     * @param context the validation context
     * @param message the failure message
     * @return false to indicate validation failure
     */
    private static boolean fail(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }

    /**
     * Normalizes the postcode by trimming, uppercasing, and collapsing whitespace.
     *
     * @param s the raw postcode
     * @return the normalized postcode
     */
    private static String normalize(String s) {
        return s.trim().toUpperCase(Locale.UK).replaceAll("\\s+", " ");
    }
}
