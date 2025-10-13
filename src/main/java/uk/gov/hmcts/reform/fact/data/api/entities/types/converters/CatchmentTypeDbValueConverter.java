package uk.gov.hmcts.reform.fact.data.api.entities.types.converters;

import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;

import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CatchmentTypeDbValueConverter extends AbstractDbValueConverter<CatchmentType> {
    CatchmentTypeDbValueConverter() {
        super(CatchmentType.values());
    }
}
