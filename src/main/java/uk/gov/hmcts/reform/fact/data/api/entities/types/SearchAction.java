package uk.gov.hmcts.reform.fact.data.api.entities.types;

/**
 * The action supplied by the frontend. Limited use on the API.
 * If nearest is supplied, the search logic will return nearest 10.
 * Otherwise, it ignores it for now. This may change in the future, however.
 */
public enum SearchAction {
    NEAREST,
    DOCUMENTS,
    UPDATE
}
