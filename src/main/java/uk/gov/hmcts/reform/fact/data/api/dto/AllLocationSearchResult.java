package uk.gov.hmcts.reform.fact.data.api.dto;

import java.util.UUID;

/**
 * Identifies a court or service centre returned by the globally ranked location search.
 */
public interface AllLocationSearchResult {

    UUID getId();

    String getLocationType();
}
