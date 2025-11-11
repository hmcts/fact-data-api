package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestingSupportControllerTest {

    private static final String COURT_NAME_PREFIX = "TESTCOURT";
    private static final String BLANK_PREFIX = " ";

    @Mock
    private CourtService courtService;

    @InjectMocks
    private TestingSupportController testingSupportController;

    @Test
    void deleteCourtsByNamePrefixReturns200() {
        when(courtService.deleteCourtsByNamePrefix(COURT_NAME_PREFIX)).thenReturn(3L);

        ResponseEntity<String> response = testingSupportController.deleteCourtsByNamePrefix(COURT_NAME_PREFIX);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("3 court(s)").contains(COURT_NAME_PREFIX);
        verify(courtService).deleteCourtsByNamePrefix(COURT_NAME_PREFIX);
    }

    @Test
    void deleteCourtsByNamePrefixPropagatesIllegalArgumentExceptionForBlankPrefix() {
        when(courtService.deleteCourtsByNamePrefix(BLANK_PREFIX)).thenThrow(new IllegalArgumentException("Invalid"));

        assertThrows(IllegalArgumentException.class, () ->
            testingSupportController.deleteCourtsByNamePrefix(BLANK_PREFIX)
        );
    }
}
