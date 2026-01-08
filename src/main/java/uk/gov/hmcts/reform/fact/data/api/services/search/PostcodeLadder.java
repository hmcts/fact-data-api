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

    public String minusUnitNoSpace() {
        return minusUnitNoSpace;
    }

    public String outcodeNoSpace() {
        return outcodeNoSpace;
    }

    public String areacodeNoSpace() {
        return areacodeNoSpace;
    }

    private static String areaPrefix(String s) {
        int i = 0;
        while (i < s.length() && !Character.isDigit(s.charAt(i))) {
            i++;
        }
        return i == 0 ? s : s.substring(0, i);
    }

    private static String normalizeNoSpace(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.UK).replace(" ", "");
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(minusUnitNoSpace, outcodeNoSpace, areacodeNoSpace);
    }

    @Override
    public String toString() {
        return "PostcodeLadder["
            + "  minusUnitNoSpace=" + minusUnitNoSpace
            + ", outcodeNoSpace=" + outcodeNoSpace
            + ", areacodeNoSpace=" + areacodeNoSpace
            + ']';
    }
}
