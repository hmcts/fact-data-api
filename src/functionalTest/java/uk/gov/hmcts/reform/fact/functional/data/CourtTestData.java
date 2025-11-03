package uk.gov.hmcts.reform.fact.functional.data;

import lombok.Data;

@Data
public final class CourtTestData {

    private String name;
    private String regionId;
    private Boolean isServiceCentre;
    private String slug;
    private Boolean open;
    private String warningNotice;
    private Boolean openOnCath;
    private String mrdId;
}
