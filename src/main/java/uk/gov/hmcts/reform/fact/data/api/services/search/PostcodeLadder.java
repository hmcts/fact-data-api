package uk.gov.hmcts.reform.fact.data.api.services.search;

import java.util.Locale;
import java.util.Objects;

public final class PostcodeLadder {

    private final String minusUnitNoSpace;
    private final String outcodeNoSpace;
    private final String areacodeNoSpace;

    private PostcodeLadder(
        String minusUnitNoSpace,
        String outcodeNoSpace,
        String areacodeNoSpace
    ) {
        this.minusUnitNoSpace = minusUnitNoSpace;
        this.outcodeNoSpace = outcodeNoSpace;
        this.areacodeNoSpace = areacodeNoSpace;
    }

    /**
     * Creates a ladder from a postcode for partial matching.
     *
     * @param fullPostcode the full postcode input
     * @return the ladder representation
     */
    public static PostcodeLadder fromPartialPostcode(String fullPostcode) {
        String full = normalizeNoSpace(fullPostcode);

        boolean hasUnit = full.length() >= 2
            && Character.isLetter(full.charAt(full.length() - 1))
            && Character.isLetter(full.charAt(full.length() - 2));

        String minusUnit;
        String outcode;

        if (hasUnit) {
            minusUnit = full.substring(0, full.length() - 2); // e.g. SW1A1A
            outcode   = full.substring(0, full.length() - 3); // e.g. SW1A
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
     * Returns the postcode without the unit (no spaces).
     *
     * @return the postcode without unit
     */
    public String minusUnitNoSpace() {
        return minusUnitNoSpace;
    }

    /**
     * Returns the outcode (no spaces).
     *
     * @return the outcode
     */
    public String outcodeNoSpace() {
        return outcodeNoSpace;
    }

    /**
     * Returns the area code (no spaces).
     *
     * @return the area code
     */
    public String areacodeNoSpace() {
        return areacodeNoSpace;
    }

    /**
     * Extracts the leading area prefix from a normalized postcode.
     *
     * @param s the normalized postcode
     * @return the area prefix
     */
    private static String areaPrefix(String s) {
        int i = 0;
        while (i < s.length() && !Character.isDigit(s.charAt(i))) {
            i++;
        }
        return i == 0 ? s : s.substring(0, i);
    }

    /**
     * Normalizes a postcode to uppercase without spaces.
     *
     * @param s the raw postcode
     * @return the normalized postcode
     */
    private static String normalizeNoSpace(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.UK).replace(" ", "");
    }

    /**
     * Compares another object for equality.
     *
     * @param o the object to compare
     * @return true if equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PostcodeLadder other)) {
            return false;
        }
        return minusUnitNoSpace.equals(other.minusUnitNoSpace)
            && outcodeNoSpace.equals(other.outcodeNoSpace)
            && areacodeNoSpace.equals(other.areacodeNoSpace);
    }

    /**
     * Computes the hash code.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(minusUnitNoSpace, outcodeNoSpace, areacodeNoSpace);
    }

    /**
     * Returns a string representation of the ladder.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "PostcodeLadder["
            + "  minusUnitNoSpace=" + minusUnitNoSpace
            + ", outcodeNoSpace=" + outcodeNoSpace
            + ", areacodeNoSpace=" + areacodeNoSpace
            + ']';
    }
}
