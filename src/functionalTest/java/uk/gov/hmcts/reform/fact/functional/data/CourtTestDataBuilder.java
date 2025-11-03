package uk.gov.hmcts.reform.fact.functional.data;

public final class CourtTestDataBuilder {

    private static final String DEFAULT_NAME = "Functional Test Court";
    private static final Boolean DEFAULT_IS_SERVICE_CENTRE = true;

    private String name;
    private String regionId;
    private Boolean isServiceCentre;
    private String slug;
    private Boolean open;
    private String warningNotice;
    private Boolean openOnCath;
    private String mrdId;

    private CourtTestDataBuilder() {
        this.name = DEFAULT_NAME;
        this.isServiceCentre = DEFAULT_IS_SERVICE_CENTRE;
    }

    public static CourtTestDataBuilder validCourt() {
        return new CourtTestDataBuilder();
    }

    public CourtTestDataBuilder withName(final String name) {
        this.name = name;
        return this;
    }

    public CourtTestDataBuilder withRegionId(final String regionId) {
        this.regionId = regionId;
        return this;
    }

    public CourtTestDataBuilder withIsServiceCentre(final Boolean isServiceCentre) {
        this.isServiceCentre = isServiceCentre;
        return this;
    }

    public CourtTestDataBuilder withSlug(final String slug) {
        this.slug = slug;
        return this;
    }

    public CourtTestDataBuilder withOpen(final Boolean open) {
        this.open = open;
        return this;
    }

    public CourtTestDataBuilder withWarningNotice(final String warningNotice) {
        this.warningNotice = warningNotice;
        return this;
    }

    public CourtTestDataBuilder withOpenOnCath(final Boolean openOnCath) {
        this.openOnCath = openOnCath;
        return this;
    }

    public CourtTestDataBuilder withMrdId(final String mrdId) {
        this.mrdId = mrdId;
        return this;
    }

    public CourtTestData build() {
        final var court = new CourtTestData();
        court.setName(name);
        court.setRegionId(regionId);
        court.setIsServiceCentre(isServiceCentre);
        court.setSlug(slug);
        court.setOpen(open);
        court.setWarningNotice(warningNotice);
        court.setOpenOnCath(openOnCath);
        court.setMrdId(mrdId);
        return court;
    }
}
