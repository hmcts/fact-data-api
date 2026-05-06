package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.CsvService;

import java.util.List;

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

    @GetMapping("/files")
    public ResponseEntity<List<String>> getCsvFiles() {
        return ResponseEntity.ok(csvService.getCsvFiles());
    }
}
