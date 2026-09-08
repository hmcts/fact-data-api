package uk.gov.hmcts.reform.fact.data.api.os;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.fact.data.api.config.OsClientConfiguration;

@FeignClient(
    name = "os-feign-client",
    url = "${os.url}",
    configuration = OsClientConfiguration.class
)
public interface OsFeignClient {

    @GetMapping("${os.endpoint.postcode-search}")
    OsData getOsPostcodeData(@RequestParam("postcode") String postcode);

    @GetMapping("${os.endpoint.postcode-search}")
    OsData getOsPostcodeDataWithMaxResultsLimit(
        @RequestParam("postcode") String postcode,
        @RequestParam("maxresults") int maxResults
    );

    /**
     * Retrieves one page of DPA and LPI records for the admin address picker.
     *
     * @param postcode full postcode to search for
     * @param dataset OS datasets to include
     * @param language language used for LPI records
     * @param maxResults maximum number of records in the page
     * @param offset first record to return
     * @return one page of OS address records
     */
    @GetMapping("${os.endpoint.postcode-search}")
    OsData getOsAdminPostcodeData(
        @RequestParam("postcode") String postcode,
        @RequestParam("dataset") String dataset,
        @RequestParam("lr") String language,
        @RequestParam("maxresults") int maxResults,
        @RequestParam("offset") int offset
    );
}
