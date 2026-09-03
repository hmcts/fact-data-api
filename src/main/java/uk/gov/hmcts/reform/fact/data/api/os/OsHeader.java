package uk.gov.hmcts.reform.fact.data.api.os;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Paging details returned with an OS Places response. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OsHeader {
    private String dataset;
    private Integer totalresults;
    private Integer maxresults;
    private Integer offset;
}
