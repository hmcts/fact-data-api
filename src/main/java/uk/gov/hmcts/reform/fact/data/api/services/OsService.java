package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsFeignClient;

@Service
public class OsService {

    private final OsFeignClient osFeignClient;

    public OsService(OsFeignClient osFeignClient) {
        this.osFeignClient = osFeignClient;
    }

    public OsData getPostcodeData(String postcode) {
        return osFeignClient.getOsPostcodeData(postcode.trim());
    }
}
