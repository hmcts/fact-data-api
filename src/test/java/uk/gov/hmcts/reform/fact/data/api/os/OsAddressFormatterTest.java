package uk.gov.hmcts.reform.fact.data.api.os;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;

class OsAddressFormatterTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "JANE DOE STREET | Jane Doe Street",
        "HMCTS BUILDING | Hmcts Building",
        "10A HIGH STREET | 10A High Street",
        "KING'S ROAD | King's Road",
        "STOKE-ON-TRENT | Stoke-On-Trent",
        "ST. JOHN’S ROAD | St. John’s Road",
        "ÉLY | Ély",
        "ST. JOHN'S ROAD / NORTH & SOUTH | St. John's Road / North & South"
    })
    void shouldTitleCaseAddressText(String value, String expected) {
        assertThat(OsAddressFormatter.toTitleCase(value)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRetainMissingText(String value) {
        assertThat(OsAddressFormatter.toTitleCase(value)).isEqualTo(value);
    }

    @Test
    void shouldFormatDpaAddressFieldsWithoutChangingPostcodeOrIdentifiers() {
        OsDpa dpa = OsDpa.builder()
            .address("HMCTS, 10A KING'S ROAD, LONDON, SW1A 1AA")
            .organisationName("HMCTS")
            .buildingName("COURT HOUSE")
            .buildingNumber("10A")
            .thoroughfareName("KING'S ROAD")
            .postTown("LONDON")
            .postcode("SW1A 1AA")
            .uprn("UPRN-123")
            .udprn("UDPRN-456")
            .lat(51.5)
            .lng(-0.1)
            .localCustodianCode(123)
            .localCustodianCodeDescription("LONDON")
            .build();

        OsAddressFormatter.format(OsResult.builder().dpa(dpa).build());

        assertThat(dpa).isEqualTo(OsDpa.builder()
            .address("Hmcts, 10A King's Road, London, SW1A 1AA")
            .organisationName("Hmcts")
            .buildingName("Court House")
            .buildingNumber("10A")
            .thoroughfareName("King's Road")
            .postTown("London")
            .postcode("SW1A 1AA")
            .uprn("UPRN-123")
            .udprn("UDPRN-456")
            .lat(51.5)
            .lng(-0.1)
            .localCustodianCode(123)
            .localCustodianCodeDescription("LONDON")
            .build());
    }

    @Test
    void shouldFormatLpiAddressFieldsWithoutChangingPostcodeOrIdentifiers() {
        OsLpi lpi = OsLpi.builder()
            .address("FLAT A, COURT HOUSE, KING'S ROAD, LONDON, SW1A 1AA")
            .organisation("HMCTS")
            .saoText("FLAT A")
            .paoText("COURT HOUSE")
            .streetDescription("KING'S ROAD")
            .localityName("WESTMINSTER")
            .townName("LONDON")
            .administrativeArea("GREATER LONDON")
            .postcodeLocator("SW1A 1AA")
            .uprn("UPRN-123")
            .lpiKey("LPI-456")
            .saoStartNumber("1")
            .saoStartSuffix("A")
            .saoEndNumber("2")
            .saoEndSuffix("B")
            .paoStartNumber("10")
            .paoStartSuffix("C")
            .paoEndNumber("12")
            .paoEndSuffix("D")
            .lat(51.5)
            .lng(-0.1)
            .build();
        OsLpi expected = OsLpi.builder()
            .address("Flat A, Court House, King's Road, London, SW1A 1AA")
            .organisation("Hmcts")
            .saoText("Flat A")
            .paoText("Court House")
            .streetDescription("King's Road")
            .localityName("Westminster")
            .townName("London")
            .administrativeArea("Greater London")
            .postcodeLocator("SW1A 1AA")
            .uprn("UPRN-123")
            .lpiKey("LPI-456")
            .saoStartNumber("1")
            .saoStartSuffix("A")
            .saoEndNumber("2")
            .saoEndSuffix("B")
            .paoStartNumber("10")
            .paoStartSuffix("C")
            .paoEndNumber("12")
            .paoEndSuffix("D")
            .lat(51.5)
            .lng(-0.1)
            .build();

        OsAddressFormatter.format(OsResult.builder().lpi(lpi).build());

        assertThat(lpi).isEqualTo(expected);
    }

    @Test
    void shouldHandleMissingOptionalFieldsAndDatasets() {
        OsAddressFormatter.format(OsResult.builder().build());
        OsDpa dpa = OsDpa.builder().address("HIGH STREET").build();
        OsLpi lpi = OsLpi.builder().address("COURT HOUSE").build();

        OsAddressFormatter.format(OsResult.builder().dpa(dpa).lpi(lpi).build());

        assertThat(dpa.getAddress()).isEqualTo("High Street");
        assertThat(dpa.getBuildingName()).isNull();
        assertThat(lpi.getAddress()).isEqualTo("Court House");
        assertThat(lpi.getTownName()).isNull();
    }
}
