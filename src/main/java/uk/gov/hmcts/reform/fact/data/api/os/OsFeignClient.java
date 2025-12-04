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
}
