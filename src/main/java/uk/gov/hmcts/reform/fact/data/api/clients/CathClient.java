package uk.gov.hmcts.reform.fact.data.api.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.hmcts.reform.fact.data.api.clients.config.CathClientConfiguration;

import java.util.Map;

@FeignClient(
    name = "cathClient",
    url = "${clients.cath.url}",
    configuration = CathClientConfiguration.class
)
public interface CathClient {

    @PutMapping(value = "/location/fact/{mrdId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    void notifyCourtStatusChange(@PathVariable("mrdId") String mrdId,
                                 @RequestBody Map<String, Boolean> request);
}
