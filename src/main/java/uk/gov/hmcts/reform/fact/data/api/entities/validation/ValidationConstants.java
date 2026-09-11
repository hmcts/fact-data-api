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
}
