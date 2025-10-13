package uk.gov.hmcts.reform.fact.data.api.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "local_authority_types")
public class LocalAuthorityType extends BaseEntity {

    @Schema(description = "The name of the Local Authority Type")
    @NotBlank(message = "Local Authority Type name must be specified")
    private String name;

}
