package uk.gov.hmcts.reform.fact.data.api.migration.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;

class WarningNoticeSanitiserTest {

    @Test
    void shouldConvertLegacyHtmlAndEntitiesToPlainText() {
        String value = """
            For queries, email <a href="mailto:notices&#64;example.com">
            notices&#64;example.com</a>.
            """;

        String result = WarningNoticeSanitiser.sanitise(value, "example-court", "English");

        assertThat(result).isEqualTo(
            "For queries, email notices@example.com."
        );
    }

    @Test
    void shouldNormaliseWhitespaceAndTypographicPunctuation() {
        String value = """
            <strong>The Chair Lift is Out of Order.</strong>\r
            Cases will move to Magistrates’ Court – please don’t attend…
            """;

        String result = WarningNoticeSanitiser.sanitise(value, "example-court", "English");

        assertThat(result).isEqualTo(
            "The Chair Lift is Out of Order. Cases will move to Magistrates' Court - please don't attend..."
        );
    }

    @Test
    void shouldPreserveWelshLetters() {
        String result = WarningNoticeSanitiser.sanitise(
            "<strong>Rhybudd:</strong> Mae’r llys yng Nghaerdydd wedi cau dros dro.",
            "example-court",
            "Welsh"
        );

        assertThat(result).isEqualTo("Rhybudd: Mae'r llys yng Nghaerdydd wedi cau dros dro.");
    }

    @Test
    void shouldProduceValuesAcceptedByTargetEntityConstraints() {
        String english = WarningNoticeSanitiser.sanitise(
            "<p>Don’t attend – email notices&#64;example.com…</p>",
            "example-court",
            "English"
        );
        String welsh = WarningNoticeSanitiser.sanitise(
            "<p>Peidiwch â dod – e-bostiwch notices&#64;example.com…</p>",
            "example-court",
            "Welsh"
        );

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(validator.validateValue(Court.class, "warningNotice", english)).isEmpty();
            assertThat(validator.validateValue(Court.class, "warningNoticeCy", welsh)).isEmpty();
            assertThat(validator.validateValue(ServiceCentre.class, "warningNotice", english)).isEmpty();
            assertThat(validator.validateValue(ServiceCentre.class, "warningNoticeCy", welsh)).isEmpty();
        }
    }

    @Test
    void shouldMapBlankNoticeToNull() {
        assertThat(WarningNoticeSanitiser.sanitise("  ", "example-court", "English")).isNull();
        assertThat(WarningNoticeSanitiser.sanitise(null, "example-court", "Welsh")).isNull();
    }
}
