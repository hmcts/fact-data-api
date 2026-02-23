package uk.gov.hmcts.reform.fact.data.api.entities.types;

import java.io.Serializable;

/**
 * Simple record for recording value change to a field.
 *
 * @param field    The field name
 * @param oldValue The previous value for the field
 * @param newValue The new value for the field
 */
public record Change(
    String field,
    Serializable oldValue,
    Serializable newValue) implements Serializable {
}
