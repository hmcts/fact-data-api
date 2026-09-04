package uk.gov.hmcts.reform.fact.data.api.os;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OsLpi {
    @JsonProperty("UPRN")
    private String uprn;

    @JsonProperty("ADDRESS")
    private String address;

    @JsonProperty("LPI_KEY")
    private String lpiKey;

    @JsonProperty("ORGANISATION")
    private String organisation;

    @JsonProperty("SAO_START_NUMBER")
    private String saoStartNumber;

    @JsonProperty("SAO_START_SUFFIX")
    private String saoStartSuffix;

    @JsonProperty("SAO_END_NUMBER")
    private String saoEndNumber;

    @JsonProperty("SAO_END_SUFFIX")
    private String saoEndSuffix;

    @JsonProperty("SAO_TEXT")
    private String saoText;

    @JsonProperty("PAO_START_NUMBER")
    private String paoStartNumber;

    @JsonProperty("PAO_START_SUFFIX")
    private String paoStartSuffix;

    @JsonProperty("PAO_END_NUMBER")
    private String paoEndNumber;

    @JsonProperty("PAO_END_SUFFIX")
    private String paoEndSuffix;

    @JsonProperty("PAO_TEXT")
    private String paoText;

    @JsonProperty("STREET_DESCRIPTION")
    private String streetDescription;

    @JsonProperty("LOCALITY_NAME")
    private String localityName;

    @JsonProperty("TOWN_NAME")
    private String townName;

    @JsonProperty("ADMINISTRATIVE_AREA")
    private String administrativeArea;

    @JsonProperty("POSTCODE_LOCATOR")
    private String postcodeLocator;

    @JsonProperty("LNG")
    private Double lng;

    @JsonProperty("LAT")
    private Double lat;
}
