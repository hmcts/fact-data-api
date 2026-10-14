package uk.gov.hmcts.reform.fact.data.api.os;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OsAddressFormatter {

    public static void format(OsResult result) {
        OsDpa dpa = result.getDpa();
        if (dpa != null) {
            dpa.setAddress(toTitleCase(dpa.getAddress(), dpa.getPostcode()));
            dpa.setOrganisationName(toTitleCase(dpa.getOrganisationName()));
            dpa.setBuildingName(toTitleCase(dpa.getBuildingName()));
            dpa.setThoroughfareName(toTitleCase(dpa.getThoroughfareName()));
            dpa.setPostTown(toTitleCase(dpa.getPostTown()));
        }

        OsLpi lpi = result.getLpi();
        if (lpi != null) {
            lpi.setAddress(toTitleCase(lpi.getAddress(), lpi.getPostcodeLocator()));
            lpi.setOrganisation(toTitleCase(lpi.getOrganisation()));
            lpi.setSaoText(toTitleCase(lpi.getSaoText()));
            lpi.setPaoText(toTitleCase(lpi.getPaoText()));
            lpi.setStreetDescription(toTitleCase(lpi.getStreetDescription()));
            lpi.setLocalityName(toTitleCase(lpi.getLocalityName()));
            lpi.setTownName(toTitleCase(lpi.getTownName()));
            lpi.setAdministrativeArea(toTitleCase(lpi.getAdministrativeArea()));
        }
    }

    private static String toTitleCase(String address, String postcode) {
        String formatted = toTitleCase(address);
        if (formatted == null || postcode == null || postcode.isBlank()) {
            return formatted;
        }

        Pattern postcodePattern = Pattern.compile(
            "(?<![\\p{L}\\p{N}])" + Pattern.quote(postcode) + "(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );
        return postcodePattern.matcher(formatted)
            .replaceAll(Matcher.quoteReplacement(postcode));
    }

    static String toTitleCase(String value) {
        if (value == null) {
            return null;
        }

        String lowerCase = value.toLowerCase(Locale.ROOT);
        StringBuilder formatted = new StringBuilder(lowerCase.length());
        boolean capitaliseNext = true;
        for (int offset = 0; offset < lowerCase.length();) {
            int character = lowerCase.codePointAt(offset);
            if (Character.isLetter(character)) {
                formatted.appendCodePoint(capitaliseNext ? Character.toUpperCase(character) : character);
                capitaliseNext = false;
            } else {
                formatted.appendCodePoint(character);
                // Keep possessives intact and allow a capitalised suffix in house numbers such as 10A.
                if (!Character.isDigit(character) && character != '\'' && character != 0x2019) {
                    capitaliseNext = true;
                }
            }
            offset += Character.charCount(character);
        }
        return formatted.toString();
    }
}
