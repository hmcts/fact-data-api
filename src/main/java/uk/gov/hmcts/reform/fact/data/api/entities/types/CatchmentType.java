package uk.gov.hmcts.reform.fact.data.api.entities.types;

import uk.gov.hmcts.reform.fact.data.api.entities.types.converters.HasDbValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// TODO: placeholder enum
@Getter
@RequiredArgsConstructor
public enum CatchmentType implements HasDbValue {
    LOCAL("local"),
    NATIONAL("national");

    private final String dbValue;
}
