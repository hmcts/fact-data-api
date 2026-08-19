package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.services.CsvService;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CsvControllerTest {

    @Mock
    private CsvService csvService;

    @InjectMocks
    private CsvController csvController;

    @Test
    void createAndUploadCsvShouldDelegateToService() {
        csvController.createAndUploadCsv();

        verify(csvService).createAndUploadCsv();
    }
}
