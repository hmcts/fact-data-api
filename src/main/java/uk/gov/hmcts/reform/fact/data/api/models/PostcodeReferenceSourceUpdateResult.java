package uk.gov.hmcts.reform.fact.data.api.models;

public record PostcodeReferenceSourceUpdateResult(
    boolean updated,
    String sourceVersion,
    String release,
    long recordCount,
    String blobPath
) {

    public static PostcodeReferenceSourceUpdateResult unchanged(
        String sourceVersion,
        String release,
        String blobPath
    ) {
        return new PostcodeReferenceSourceUpdateResult(false, sourceVersion, release, 0, blobPath);
    }

    public static PostcodeReferenceSourceUpdateResult updated(
        String sourceVersion,
        String release,
        long recordCount,
        String blobPath
    ) {
        return new PostcodeReferenceSourceUpdateResult(true, sourceVersion, release, recordCount, blobPath);
    }
}
