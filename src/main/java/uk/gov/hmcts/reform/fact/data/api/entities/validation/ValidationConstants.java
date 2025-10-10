package uk.gov.hmcts.reform.fact.data.api.entities.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ValidationConstants {

    public static final String EMAIL_REGEX = "^(|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
    public static final String EMAIL_REGEX_MESSAGE =
        "Email address must match the regex '^(|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})$'";
    public static final int EMAIL_MAX_LENGTH = 254;
    public static final String EMAIL_MAX_LENGTH_MESSAGE = "Email address should be no more than {max} characters";

    public static final String PHONE_NO_REGEX = "^(|[0-9 ]{10,20})$";
    public static final String PHONE_NO_REGEX_MESSAGE =
        "Phone Number must match the regex '^(|[0-9 ]{10,20})$'";
    public static final int PHONE_NO_MAX_LENGTH = 20;
    public static final String PHONE_NO_MAX_LENGTH_MESSAGE = "Phone number should be no more than {max} characters";

    public static final String POSTCODE_REGEX = "^([a-zA-Z]{1,2}\\d{1,2})\\s*?(\\d[a-zA-Z]{2})$";
    public static final String POSTCODE_REGEX_MESSAGE =
        "Postcode must match the regex '^([a-zA-Z]{1,2}\\d{1,2})\\s*?(\\d[a-zA-Z]{2})$'";
    public static final int POSTCODE_MAX_LENGTH = 8;
    public static final String POSTCODE_MAX_LENGTH_MESSAGE = "Postcode should be no more than {max} characters";

}

