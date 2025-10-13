package uk.gov.hmcts.reform.fact.data.api.entities.types;

import uk.gov.hmcts.reform.fact.data.api.entities.types.converters.HasDbValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// TODO: placeholder enum
@Getter
@RequiredArgsConstructor
public enum ServiceAreaType implements HasDbValue {

    CIVIL("civil"),
    FAMILY("family"),
    OTHER("other");

    private final String dbValue;
}
