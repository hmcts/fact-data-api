package uk.gov.hmcts.reform.fact.data.api.migration.entities;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.UUID;

/**
 * Minimal entity used by the migration helper to persist the legacy "service" data into the
 * existing table. Deliberately lives inside the migration package so it can be removed with the
 * helper once the migration code is retired.
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@Table(name = "service")
public class LegacyService {

    @Schema(
        description = "The internal ID - assigned by the server during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Schema(description = "The name")
    @NotBlank(message = "The name must be specified")
    private String name;

    @Schema(description = "The Welsh language name")
    @NotBlank(message = "The Welsh language name must be specified")
    private String nameCy;

    @Schema(description = "The description")
    private String description;

    @Schema(description = "The Welsh language description")
    private String descriptionCy;

    @Schema(description = "Associated service areas")
    @Type(ListArrayType.class)
    @Column(name = "service_areas", columnDefinition = "uuid[]")
    private List<UUID> serviceAreas;
}
