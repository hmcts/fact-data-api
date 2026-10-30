package uk.gov.hmcts.reform.fact.data.api.models;

public record PostcodeReferenceImportMetadata(
    String sourceVersion,
    String release,
    String blobEtag,
    long recordCount
) {
}
