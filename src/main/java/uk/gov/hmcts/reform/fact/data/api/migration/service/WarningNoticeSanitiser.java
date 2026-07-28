package uk.gov.hmcts.reform.fact.data.api.migration.service;

import java.util.Objects;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.HtmlUtils;

final class WarningNoticeSanitiser {

    private static final Logger LOG = LoggerFactory.getLogger(WarningNoticeSanitiser.class);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern WHITESPACE_BEFORE_PUNCTUATION = Pattern.compile("\\s+([.,!?:;])");

    private WarningNoticeSanitiser() {
    }

    static String sanitise(String value, String courtSlug, String language) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        String sanitised = HtmlUtils.htmlUnescape(value);
        sanitised = HTML_TAG_PATTERN.matcher(sanitised).replaceAll(" ")
            .replace('‘', '\'')
            .replace('’', '\'')
            .replace('“', '"')
            .replace('”', '"')
            .replace("–", "-")
            .replace("—", "-")
            .replace("…", "...");
        sanitised = StringUtils.normalizeSpace(sanitised);
        sanitised = WHITESPACE_BEFORE_PUNCTUATION.matcher(sanitised).replaceAll("$1");

        if (!Objects.equals(value, sanitised)) {
            LOG.info("Sanitised {} warning notice for legacy court '{}'", language, courtSlug);
        }
        return StringUtils.defaultIfBlank(sanitised, null);
    }
}
