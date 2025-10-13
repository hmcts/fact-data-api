package uk.gov.hmcts.reform.fact.data.api.entities.types.converters;

import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentMethod;

import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CatchmentMethodDbValueConverter extends AbstractDbValueConverter<CatchmentMethod> {
    CatchmentMethodDbValueConverter() {
        super(CatchmentMethod.values());
    }
}
