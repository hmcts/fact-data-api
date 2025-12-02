package uk.gov.hmcts.reform.fact.data.api.os;

import java.util.List;

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
public class OsData {

    private List<Result> results;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Result {

        @JsonProperty("DPA")
        private Dpa dpa;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Dpa {

        @JsonProperty("UPRN")
        private String uprn;

        @JsonProperty("UDPRN")
        private String udprn;

        @JsonProperty("ADDRESS")
        private String address;

        @JsonProperty("BUILDING_NUMBER")
        private String buildingNumber;

        @JsonProperty("THOROUGHFARE_NAME")
        private String thoroughfareName;

        @JsonProperty("POST_TOWN")
        private String postTown;

        @JsonProperty("POSTCODE")
        private String postcode;

        @JsonProperty("LNG")
        private double lng;

        @JsonProperty("LAT")
        private double lat;

        @JsonProperty("LOCAL_CUSTODIAN_CODE")
        private Integer localCustodianCode;

        @JsonProperty("LOCAL_CUSTODIAN_CODE_DESCRIPTION")
        private String localCustodianCodeDescription;
    }
}
