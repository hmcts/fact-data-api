package uk.gov.hmcts.reform.fact.data.api.os;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
    private OsHeader header;
    private List<OsResult> results;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private OsPostcodeMetadata postcodeMetadata;
}
