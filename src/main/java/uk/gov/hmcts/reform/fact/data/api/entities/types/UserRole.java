package uk.gov.hmcts.reform.fact.data.api.entities.types;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    ADMIN("Admin"),
    SUPER_ADMIN("SuperAdmin"),
    VIEWER("Viewer");

    private final String label;

    @JsonCreator
    public static UserRole from(String value) {
        return Arrays.stream(values())
            .filter(role -> role.name().equals(value) || role.label.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid user role: " + value));
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
