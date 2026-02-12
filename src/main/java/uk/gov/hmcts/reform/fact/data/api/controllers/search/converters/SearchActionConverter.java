package uk.gov.hmcts.reform.fact.data.api.controllers.search.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;

@Component
public class SearchActionConverter implements Converter<String, SearchAction> {

    /**
     * Converts a string value into a {@link SearchAction}.
     *
     * @param source the raw action value
     * @return the parsed {@link SearchAction}
     */
    @Override
    public SearchAction convert(String source) {
        // In case we receive nearest, rather than NEAREST, for example
        return SearchAction.valueOf(source.trim().toUpperCase());
    }
}
