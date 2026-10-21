package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceImportResult;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceSourceUpdateResult;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceSynchronisationResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostcodeReferenceSynchronisationServiceTest {

    @Mock
    private PostcodeReferenceSourceService sourceService;

    @Mock
    private PostcodeReferenceImportService importService;

    @Test
    void importsDatabaseEvenWhenSourceBlobIsAlreadyCurrent() {
        when(sourceService.updateIfRequired()).thenReturn(PostcodeReferenceSourceUpdateResult.unchanged(
            "aug:3", "2026-08", "onspd/postcode-reference.csv"));
        Instant importedAt = Instant.parse("2026-10-20T09:00:00Z");
        when(importService.importLatest()).thenReturn(new PostcodeReferenceImportResult(
            true, "aug:3", "2026-08", 993_979, importedAt));

        PostcodeReferenceSynchronisationResult result =
            new PostcodeReferenceSynchronisationService(sourceService, importService).synchronise();

        assertThat(result.blobUpdated()).isFalse();
        assertThat(result.imported()).isTrue();
        assertThat(result.recordCount()).isEqualTo(993_979);
        assertThat(result.importedAt()).isEqualTo(importedAt);
    }
}
