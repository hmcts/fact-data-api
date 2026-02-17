package uk.gov.hmcts.reform.fact.data.api.controllers.search.converters;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchActionConverterTest {

    private final SearchActionConverter converter = new SearchActionConverter();

    @Test
    void convertShouldTrimAndUppercase() {
        SearchAction result = converter.convert("  nearest  ");

        assertThat(result).isEqualTo(SearchAction.NEAREST);
    }

    @Test
    void convertShouldThrowForUnknownAction() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert("not-an-action"));
    }
}
