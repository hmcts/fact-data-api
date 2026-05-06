package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.CsvService;

@SecuredFactRestController(
    name = "CSV",
    description = "Operations related to CSV file handling"
)
@RequestMapping("/csv")
public class CsvController {

    private final CsvService csvService;

    public CsvController(CsvService csvService) {
        this.csvService = csvService;
    }

    @PostMapping("/files")
    public void createCsvFiles() {
        csvService.createCsvFiles();
    }
}
