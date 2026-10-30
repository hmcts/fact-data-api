package uk.gov.hmcts.reform.fact.data.api.security;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.security.access.prepost.PreAuthorize;
import uk.gov.hmcts.reform.fact.data.api.controllers.PostcodeReferenceController;

import static org.assertj.core.api.Assertions.assertThat;

class PostcodeReferenceControllerSecurityTest {

    @Test
    void importRequiresAdminRole() {
        assertThat(MergedAnnotations.from(PostcodeReferenceController.class)
            .get(PreAuthorize.class)
            .getString("value"))
            .isEqualTo("@authService.isAdmin()");
    }
}
