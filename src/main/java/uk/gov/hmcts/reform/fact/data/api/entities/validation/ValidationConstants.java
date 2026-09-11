package uk.gov.hmcts.reform.fact.data.api.entities.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ValidationConstants {

    public static final int COURT_NAME_MIN_LENGTH = 5;
    public static final int COURT_NAME_MAX_LENGTH = 200;
    public static final String COURT_NAME_LENGTH_MESSAGE =
        "Court name should be between {min} and {max} chars";
    public static final String COURT_NAME_REGEX = "^[A-Za-z&'()\\- ]+$";
    public static final String COURT_NAME_REGEX_MESSAGE =
        "Court or tribunal name must only include letters, spaces, brackets, apostrophes, hyphens and ampersands";

    public static final String EMAIL_REGEX = "^(|[A-Za-z0-9._+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,})$";
    public static final String EMAIL_REGEX_MESSAGE =
        "Email address must match the regex '^(|[A-Za-z0-9._+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,})$'";

    public static final String JUSTICE_EMAIL_REGEX = "^(|[A-Za-z0-9._+-]+@(devl\\.)?justice\\.gov\\.uk)$";
    public static final String JUSTICE_EMAIL_REGEX_MESSAGE =
        "Justice email address must match the regex '^(|[A-Za-z0-9._+-]+@(devl\\.)?justice\\.gov\\.uk)$'";

    public static final int EMAIL_MAX_LENGTH = 254;
    public static final String EMAIL_MAX_LENGTH_MESSAGE = "Email address should be no more than {max} characters";

    public static final String PHONE_NO_REGEX = "^(|(\\+44)?[0-9 ()-]{10,20})$";
    public static final String PHONE_NO_REGEX_MESSAGE =
        "Phone Number must match the regex '^(|(\\+44)?[0-9 ()-]{10,20})$'";

    public static final int PHONE_NO_MAX_LENGTH = 20;
    public static final String PHONE_NO_MAX_LENGTH_MESSAGE = "Phone number should be no more than {max} characters";

    public static final String ENGLISH_TEXT_REGEX = "^[A-Za-z0-9 ()':,\\-;.]+$";
    public static final String ENGLISH_TEXT_REGEX_MESSAGE = "Value contains invalid characters";

    public static final String WELSH_TEXT_REGEX = "^[\\p{L}\\p{M}0-9 ()':,\\-;.]+$";
    public static final String WELSH_TEXT_REGEX_MESSAGE =
        "Description in Welsh must only include letters, spaces, apostrophes, hyphens, ampersands, and parentheses";

    public static final String OPTIONAL_ADDRESS_LINE_REGEX = "^(|[A-Za-z0-9 ()':,.-]+$)$";
    public static final String REQUIRED_ADDRESS_LINE_REGEX = "^[A-Za-z0-9 ()':,.-]+$";
    public static final String ADDRESS_LINE_REGEX_MESSAGE = "Address line contains invalid characters";

    public static final String EPIM_ID_REGEX = "^[A-Za-z0-9 -]+$";
    public static final String EPIM_ID_REGEX_MESSAGE = "EPIM ID contains invalid characters";

    public static final String COURT_SLUG_REGEX = "^[a-z0-9-]+$";
    public static final String COURT_SLUG_REGEX_MESSAGE =
        "Slug must match the regex '^[a-z0-9-]+$'";

    public static final int COURT_SLUG_MIN_LENGTH = 1;
    public static final int COURT_SLUG_MAX_LENGTH = 250;
    public static final String COURT_SLUG_LENGTH_MESSAGE = "Court slug should be between 1 and 250 characters";

    public static final int WARNING_NOTICE_MAX_LENGTH = 250;
    public static final String WARNING_NOTICE_MAX_LENGTH_MESSAGE =
        "Warning notice must be less than {max} characters";
    public static final String WELSH_WARNING_NOTICE_MAX_LENGTH_MESSAGE =
        "Welsh warning notice must be less than {max} characters";

    public static final String WARNING_NOTICE_REGEX = "^[A-Za-z0-9.,!?:;'\"()\\-/&@+\\s]+$";
    public static final String WARNING_NOTICE_REGEX_MESSAGE =
        "Warning notice may only contain letters, numbers, spaces, and standard punctuation or symbols (@, +)";

    public static final String WELSH_WARNING_NOTICE_REGEX = "^[\\p{L}0-9.,!?:;'\"()\\-/&@+\\s]+$";
    public static final String WELSH_WARNING_NOTICE_REGEX_MESSAGE =
        "Welsh warning notice may only contain letters, numbers, spaces, and standard punctuation or symbols (@, +)";

    public static final int ACCESSIBLE_TOILET_DESCRIPTION_MAX_LENGTH = 255;
    public static final String ACCESSIBLE_TOILET_DESCRIPTION_MAX_LENGTH_MESSAGE =
        "Accessible toilet description must not exceed {max} characters";
    public static final String WELSH_ACCESSIBLE_TOILET_DESCRIPTION_MAX_LENGTH_MESSAGE =
        "Welsh accessible toilet description must not exceed {max} characters";

    public static final long LIFT_DOOR_WIDTH_MIN = 1;
    public static final long LIFT_DOOR_WIDTH_MAX = 1000;
    public static final String LIFT_DOOR_WIDTH_MIN_MESSAGE = "Lift door width needs to be over {value}cm";
    public static final String LIFT_DOOR_WIDTH_MAX_MESSAGE = "Lift door width needs to be under {value}cm";

    public static final long LIFT_WEIGHT_LIMIT_MIN = 1;
    public static final long LIFT_WEIGHT_LIMIT_MAX = 10000;
    public static final String LIFT_WEIGHT_LIMIT_MIN_MESSAGE = "Lift weight limit should be at least {value}kg";
    public static final String LIFT_WEIGHT_LIMIT_MAX_MESSAGE = "Lift weight limit should be at most {value}kg";

    public static final int COURT_ADDRESS_LINE_MAX_LENGTH = 255;
    public static final int COURT_COUNTRY_TOWN_CITY_MAX_LENGTH = 100;
    public static final int COURT_EPIM_ID_MAX_LENGTH = 10;
    public static final  String COURT_ADDRESS_LINE_MAX_LENGTH_MESSAGE =
        "Address line should be {max} characters or less";
    public static final String COURT_COUNTRY_TOWN_CITY_MAX_LENGTH_MESSAGE =
        "County/Town/City name should be {max} characters or less";
    public static final String COURT_EPIM_ID_MAX_LENGTH_MESSAGE = "EPIM ID should be {max} characters or less";

    public static final int COURT_CODE_MAX_DIGITS = 6;
    public static final int COURT_CODE_FRACTION_DIGITS = 0;
    public static final String COURT_CODE_DIGITS_MESSAGE =
        "court code must be at most {integer} digits";

    public static final int GBS_CODE_MAX_LENGTH = 10;
    public static final String GBS_CODE_MAX_LENGTH_MESSAGE = "GBS code must be {max} characters or fewer";
    public static final String GBS_CODE_REGEX = "^[A-Za-z0-9 ]*$";
    public static final String GBS_CODE_REGEX_MESSAGE = "GBS code contains invalid characters";
}
