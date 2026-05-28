package uk.gov.hmcts.reform.fact.data.api.entities.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ValidationConstantsTest {

    @Test
    void shouldAllowJusticeEmailAddresses() {
        assertThat("user.name+test@justice.gov.uk")
            .matches(ValidationConstants.JUSTICE_EMAIL_REGEX);
        assertThat("user.name+test@devl.justice.gov.uk")
            .matches(ValidationConstants.JUSTICE_EMAIL_REGEX);
    }

    @Test
    void shouldAllowEmptyJusticeEmailAddress() {
        assertThat("").matches(ValidationConstants.JUSTICE_EMAIL_REGEX);
    }

    @Test
    void shouldRejectNonJusticeEmailAddresses() {
        assertThat("user.name+test@example.com")
            .doesNotMatch(ValidationConstants.JUSTICE_EMAIL_REGEX);
        assertThat("user.name+test@notdevl.justice.gov.uk")
            .doesNotMatch(ValidationConstants.JUSTICE_EMAIL_REGEX);
        assertThat("user.name+test@devljustice.gov.uk")
            .doesNotMatch(ValidationConstants.JUSTICE_EMAIL_REGEX);
    }
}
