package uk.gov.hmcts.reform.fact.data.api.entities.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ValidationConstantsTest {

    @Test
    void shouldAllowEmailAddresses() {
        assertMatches(ValidationConstants.EMAIL_REGEX, "email.test@email.com");
        assertMatches(ValidationConstants.EMAIL_REGEX, "email.test@email.co.uk");
    }

    @Test
    void shouldAllowEmptyEmailAddress() {
        assertMatches(ValidationConstants.EMAIL_REGEX, "");
    }

    @Test
    void shouldRejectEmailAddressesWithConsecutiveDotsInDomain() {
        assertDoesNotMatch(ValidationConstants.EMAIL_REGEX, "email.test@email..com");
        assertDoesNotMatch(ValidationConstants.EMAIL_REGEX, "email.test@court..justice.gov.uk");
    }

    @Test
    void shouldAllowJusticeEmailAddresses() {
        assertMatches(ValidationConstants.JUSTICE_EMAIL_REGEX, "user.name+test@justice.gov.uk");
        assertMatches(ValidationConstants.JUSTICE_EMAIL_REGEX, "user.name+test@devl.justice.gov.uk");
    }

    @Test
    void shouldAllowEmptyJusticeEmailAddress() {
        assertMatches(ValidationConstants.JUSTICE_EMAIL_REGEX, "");
    }

    @Test
    void shouldRejectNonJusticeEmailAddresses() {
        assertDoesNotMatch(ValidationConstants.JUSTICE_EMAIL_REGEX, "user.name+test@example.com");
        assertDoesNotMatch(ValidationConstants.JUSTICE_EMAIL_REGEX, "user.name+test@notdevl.justice.gov.uk");
        assertDoesNotMatch(ValidationConstants.JUSTICE_EMAIL_REGEX, "user.name+test@devljustice.gov.uk");
    }

    private static void assertMatches(String regex, String value) {
        assertThat(Pattern.matches(regex, value)).isTrue();
    }

    private static void assertDoesNotMatch(String regex, String value) {
        assertThat(Pattern.matches(regex, value)).isFalse();
    }
}
