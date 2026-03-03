package uk.gov.hmcts.reform.fact.data.api.migration.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LegacyExportResponse;

@FeignClient(
    name = "legacy-fact-export-client",
    url = "${migration.source-base-url}",
    primary = false
)
public interface LegacyFactClient {

    @GetMapping("/private-migration/export")
    LegacyExportResponse fetchExport();
}
