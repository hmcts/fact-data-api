package uk.gov.hmcts.reform.fact.data.api.services.search;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import java.util.Locale;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostcodeLadder {

    String minusUnitNoSpace;
    String outCodeNoSpace;
    String areacodeNoSpace;

    /**
     * Creates a PostcodeLadder from a postcode for partial matching.
     * The input postcode is normalised (trimmed, uppercased, spaces removed)
     * and decomposed into useful partial-match components.
     *
     * @param fullPostcode the full or partial postcode input
     * @return a populated PostcodeLadder instance
     */
    public static PostcodeLadder fromPartialPostcode(String fullPostcode) {
        String full = normalizeNoSpace(fullPostcode);

        boolean hasUnit = full.length() >= 2
            && Character.isLetter(full.charAt(full.length() - 1))
            && Character.isLetter(full.charAt(full.length() - 2));

        String minusUnit;
        String outcode;

        if (hasUnit) {
            minusUnit = full.substring(0, full.length() - 2);
            outcode   = full.substring(0, full.length() - 3);
        } else {
            minusUnit = full;
            outcode = (!full.isEmpty() && Character.isDigit(full.charAt(full.length() - 1)))
                ? full.substring(0, full.length() - 1)
                : full;
        }

        return new PostcodeLadder(
            minusUnit,
            outcode,
            areaPrefix(full)
        );
    }

    /**
     * Extracts the leading area prefix from a normalised postcode.
     * The area prefix consists of the leading letters before the first digit.
     * Examples:
     * SW1A1AA -> SW
     * B12CD   -> B
     * EC1A    -> EC
     *
     * @param s the normalised postcode (uppercase, no spaces)
     * @return the alphabetical area prefix
     */
    private static String areaPrefix(String s) {
        int i = 0;
        while (i < s.length() && !Character.isDigit(s.charAt(i))) {
            i++;
        }
        return i == 0 ? s : s.substring(0, i);
    }


    /**
     * Normalises a postcode by trimming whitespace, converting to uppercase,
     * and removing spaces.
     *
     * @param s the raw postcode input (this may be null)
     * @return a normalised postcode string, or empty string if input was null
     */
    private static String normalizeNoSpace(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.UK).replace(" ", "");
    }
}
