package uk.gov.hmcts.reform.fact.data.api.entities.types.converters;

import uk.gov.hmcts.reform.fact.data.api.entities.types.ServiceAreaType;

import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ServiceAreaTypeDbValueConverter extends AbstractDbValueConverter<ServiceAreaType> {
    ServiceAreaTypeDbValueConverter() {
        super(ServiceAreaType.values());
    }
}
