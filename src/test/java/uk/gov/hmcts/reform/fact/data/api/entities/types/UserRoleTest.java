package uk.gov.hmcts.reform.fact.data.api.entities.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UserRoleTest {

    @Test
    void shouldParseRoleLabels() {
        assertThat(UserRole.from("Admin")).isEqualTo(UserRole.ADMIN);
        assertThat(UserRole.from("SuperAdmin")).isEqualTo(UserRole.SUPER_ADMIN);
        assertThat(UserRole.from("Viewer")).isEqualTo(UserRole.VIEWER);
    }

    @Test
    void shouldParseEnumNames() {
        assertThat(UserRole.from("ADMIN")).isEqualTo(UserRole.ADMIN);
        assertThat(UserRole.from("SUPER_ADMIN")).isEqualTo(UserRole.SUPER_ADMIN);
        assertThat(UserRole.from("VIEWER")).isEqualTo(UserRole.VIEWER);
    }

    @Test
    void shouldExposeRoleLabels() {
        assertThat(UserRole.ADMIN.getLabel()).isEqualTo("Admin");
        assertThat(UserRole.SUPER_ADMIN.getLabel()).isEqualTo("SuperAdmin");
        assertThat(UserRole.VIEWER.getLabel()).isEqualTo("Viewer");
    }

    @Test
    void shouldRejectUnknownRole() {
        assertThatThrownBy(() -> UserRole.from("Unknown"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid user role: Unknown");
    }
}
