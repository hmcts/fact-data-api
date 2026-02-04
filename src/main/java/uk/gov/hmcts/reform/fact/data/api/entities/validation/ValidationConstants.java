package uk.gov.hmcts.reform.fact.data.api.entities.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ValidationConstants {

    public static final String EMAIL_REGEX = "^(|[A-Za-z0-9._+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
    public static final String EMAIL_REGEX_MESSAGE =
        "Email address must match the regex '^(|[A-Za-z0-9._+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})$'";

    public static final String JUSTICE_EMAIL_REGEX = "^(|[A-Za-z0-9._+-]+@justice\\.gov\\.uk)$";
    public static final String JUSTICE_EMAIL_REGEX_MESSAGE =
        "Justice email address must match the regex '^(|[A-Za-z0-9._+-]+@justice\\.gov\\.uk)$'";

    public static final int EMAIL_MAX_LENGTH = 254;
    public static final String EMAIL_MAX_LENGTH_MESSAGE = "Email address should be no more than {max} characters";

    public static final String PHONE_NO_REGEX = "^(|[0-9 ]{10,20})$";
    public static final String PHONE_NO_REGEX_MESSAGE =
        "Phone Number must match the regex '^(|[0-9 ]{10,20})$'";

    public static final int PHONE_NO_MAX_LENGTH = 20;
    public static final String PHONE_NO_MAX_LENGTH_MESSAGE = "Phone number should be no more than {max} characters";

    public static final String GENERIC_DESCRIPTION_REGEX = "^[A-Za-z0-9 ()':,-]+$";
    public static final String GENERIC_DESCRIPTION_REGEX_MESSAGE = "Value contains invalid characters";

    public static final String COURT_SLUG_REGEX = "^[a-z0-9-]+$";
    public static final String COURT_SLUG_REGEX_MESSAGE =
        "Slug must match the regex '^[a-z0-9-]+$'";

    public static final int COURT_SLUG_MIN_LENGTH = 5;
    public static final int COURT_SLUG_MAX_LENGTH = 250;
    public static final String COURT_SLUG_LENGTH_MESSAGE = "Court slug should be between 5 and 250 characters";

}
