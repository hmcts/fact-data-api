package uk.gov.hmcts.reform.fact.data.api.entities.types;

/**
 * Enums for search strategies:
 * DEFAULT_AOL_DISTANCE is for where individuals are searching for nearest courts.
 * FAMILY_NON_REGIONAL family area of law search for one court within a local authority.
 * FAMILY_REGIONAL family area of law search routed through regional service centres.
 * CIVIL_POSTCODE_PREFERENCE civil jurisdictional search based on postcode supplied.
 */
public enum SearchStrategy {
    DEFAULT_AOL_DISTANCE,
    FAMILY_NON_REGIONAL,
    FAMILY_REGIONAL,
    CIVIL_POSTCODE_PREFERENCE
}
