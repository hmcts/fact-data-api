package uk.gov.hmcts.reform.fact.data.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.hmcts.reform.fact.data.api.controllers.CourtContactDetailsController;
import uk.gov.hmcts.reform.fact.data.api.controllers.CourtController;
import uk.gov.hmcts.reform.fact.data.api.controllers.search.SearchAddressController;
import uk.gov.hmcts.reform.fact.data.api.controllers.search.SearchCourtController;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;

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

    @Test
    void postcodeSearchAllowsPrlViaMethodSecurity() throws NoSuchMethodException {
        assertThat(MergedAnnotations.from(SearchCourtController.class.getMethod(
            "getCourtsByPostcode",
            String.class,
            String.class,
            SearchAction.class,
            Integer.class
        ))
            .get(PreAuthorize.class)
            .getString("value"))
            .isEqualTo("@authService.canView() || @authService.isPrl()");
    }

    @Test
    void courtDetailsByIdAllowsPrlViaMethodSecurity() throws NoSuchMethodException {
        assertThat(MergedAnnotations.from(CourtController.class.getMethod("getCourtDetailsById", String.class))
            .get(PreAuthorize.class)
            .getString("value"))
            .isEqualTo("@authService.canView() || @authService.isPrl()");
    }

    @Test
    void courtDetailsBySlugAllowsPrlViaMethodSecurity() throws NoSuchMethodException {
        assertThat(MergedAnnotations.from(CourtController.class.getMethod("getCourtDetailsBySlug", String.class))
            .get(PreAuthorize.class)
            .getString("value"))
            .isEqualTo("@authService.canView() || @authService.isPrl()");
    }

    @Test
    void courtContactDetailsAllowsPrlViaMethodSecurity() throws NoSuchMethodException {
        assertThat(MergedAnnotations.from(CourtContactDetailsController.class
                                              .getMethod("getContactDetails", String.class))
            .get(PreAuthorize.class)
            .getString("value"))
            .isEqualTo("@authService.isAdmin() || @authService.isPrl()");
    }
}
