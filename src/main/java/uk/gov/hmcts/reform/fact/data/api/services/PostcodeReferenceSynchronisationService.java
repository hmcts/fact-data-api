package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceImportResult;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceSourceUpdateResult;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceSynchronisationResult;

@Service
@RequiredArgsConstructor
public class PostcodeReferenceSynchronisationService {

    private final PostcodeReferenceSourceService sourceService;
    private final PostcodeReferenceImportService importService;

    /**
     * Refreshes both blob and database state, retrying the database import even when the blob is already current.
     *
     * @return combined source and import outcome
     */
    public PostcodeReferenceSynchronisationResult synchronise() {
        PostcodeReferenceSourceUpdateResult sourceResult = sourceService.updateIfRequired();
        PostcodeReferenceImportResult importResult = importService.importLatest();
        return new PostcodeReferenceSynchronisationResult(
            sourceResult.updated(),
            importResult.imported(),
            importResult.sourceVersion(),
            importResult.release(),
            importResult.recordCount(),
            sourceResult.blobPath(),
            importResult.importedAt()
        );
    }
}
