package uk.gov.hmcts.reform.fact.data.api.entities.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum AllowedLocalAuthorityAreasOfLaw {
    ADOPTION("Adoption"),
    CHILDREN("Children"),
    CIVIL_PARTNERSHIP("Civil partnership"),
    DIVORCE("Divorce");

    private final String displayName;

    public static List<String> displayNames() {
        return List.of(
            ADOPTION.displayName,
            CHILDREN.displayName,
            CIVIL_PARTNERSHIP.displayName,
            DIVORCE.displayName
        );
    }
}
