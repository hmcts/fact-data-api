package uk.gov.hmcts.reform.fact.data.api.entities.types;

import uk.gov.hmcts.reform.fact.data.api.entities.types.converters.HasDbValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// TODO - placeholder enum
@Getter
@RequiredArgsConstructor
public enum CatchmentMethod implements HasDbValue {
    POSTCODE("postcode"),
    PROXIMITY("proximity"),
    LOCAL_AUTHORITY("local-authority");

    private final String dbValue;
}

