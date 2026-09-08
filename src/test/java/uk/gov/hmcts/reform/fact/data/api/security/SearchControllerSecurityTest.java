package uk.gov.hmcts.reform.fact.data.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.hmcts.reform.fact.data.api.controllers.search.SearchAddressController;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.security.access.prepost.PreAuthorize;

class SearchControllerSecurityTest {

    @Test
    void addressSearchRetainsAdminAuthorization() {
        assertThat(MergedAnnotations.from(SearchAddressController.class)
            .get(PreAuthorize.class)
            .getString("value"))
            .isEqualTo("@authService.isAdmin()");
    }
}
