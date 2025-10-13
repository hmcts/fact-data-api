package uk.gov.hmcts.reform.fact.data.api.entities.types.converters;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.AttributeConverter;
import lombok.NonNull;

/**
 * Abstract bi-directional conversion between database values and java objects.
 * </p>
 * {@code null} input values with return {@code null} responses. If {@code null} checking is required on a column, apply
 * it by other means.
 *
 * @param <T> a concrete implementation of {@link HasDbValue}
 */
@SuppressWarnings("ConverterNotAnnotatedInspection")
public abstract class AbstractDbValueConverter<T extends HasDbValue> implements AttributeConverter<T, String> {

    private final Map<String, T> lookup = new HashMap<>();

    protected AbstractDbValueConverter(@NonNull T[] values) {
        for (T value : values) {
            lookup.put(value.getDbValue(), value);
        }
    }

    @Override
    public String convertToDatabaseColumn(final T attribute) {
        return Optional.ofNullable(attribute).map(T::getDbValue).orElse(null);
    }

    @Override
    public T convertToEntityAttribute(final String dbData) {
        var result = Optional.ofNullable(dbData).map(lookup::get);
        if (result.isPresent()) {
            return result.get();
        } else if (dbData == null) {
            return null;
        }
        throw new IllegalArgumentException(String.format("Cannot convert db data: '%s' to entity attribute", dbData));
    }
}
