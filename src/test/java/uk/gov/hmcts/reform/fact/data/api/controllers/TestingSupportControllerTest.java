package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;
import uk.gov.hmcts.reform.fact.data.api.services.RegionService;
import uk.gov.hmcts.reform.fact.data.api.services.TestingSupportService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestingSupportControllerTest {

    private static final String COURT_NAME_PREFIX = "TESTCOURT";
    private static final String BLANK_PREFIX = " ";

    @Mock
    private CourtService courtService;
    @Mock
    private RegionService regionService;
    @Mock
    private TestingSupportService testingSupportService;

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

    @Test
    void getRegionsReturns200() {
        UUID regionId = UUID.randomUUID();
        List<Region> regions = List.of(Region.builder().id(regionId).name("London").build());
        when(regionService.getAllRegions()).thenReturn(regions);

        ResponseEntity<List<Region>> response = testingSupportController.getRegions();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactlyElementsOf(regions);
        verify(regionService).getAllRegions();
    }

    @Test
    void createSampleCourtPassesTranslationOverride() {
        when(testingSupportService.createCourt(
            anyString(),
            eq((UUID) null),
            anyLong(),
            anyBoolean(),
            anyBoolean(),
            anyBoolean(),
            anyBoolean(),
            anyBoolean()
        ))
            .thenReturn("test-court");

        testingSupportController.createSampleCourt("Test Court", null, 1L, true, false, false, false, false);

        verify(testingSupportService).createCourt("Test Court", null, 1L, true, false, false, false, false);
    }

    @Test
    void createSampleCourtPassesRegionIdOverride() {
        UUID regionId = UUID.randomUUID();
        when(testingSupportService.createCourt(
            anyString(),
            eq(regionId),
            anyLong(),
            anyBoolean(),
            anyBoolean(),
            anyBoolean(),
            anyBoolean(),
            anyBoolean()
        ))
            .thenReturn("test-court");

        testingSupportController.createSampleCourt("Test Court", regionId, 1L, true, false, false, false, false);

        verify(testingSupportService).createCourt("Test Court", regionId, 1L, true, false, false, false, false);
    }
}
