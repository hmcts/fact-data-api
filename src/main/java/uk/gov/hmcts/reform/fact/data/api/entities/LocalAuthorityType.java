package uk.gov.hmcts.reform.fact.data.api.entities;

import java.util.List;
import java.util.UUID;

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

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@Table(name = "local_authority_types")
public class LocalAuthorityType {

    @Schema(
        description = "The internal ID - assigned by the server during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Schema(description = "The name of the Local Authority Type")
    @NotBlank(message = "Local Authority Type name must be specified")
    private String name;

    @Schema(description = "LOCAL_CUSTODIAN_CODE from Ordnance Survey")
    @Column(name = "custodian_code", nullable = false, unique = true)
    private Integer custodianCode;

    @Type(ListArrayType.class)
    @Schema(description = "Child custodian codes that should map to this parent authority, if any")
    @Column(
        name = "child_custodian_codes",
        nullable = false,
        columnDefinition = "integer[]"
    )
    private List<Integer> childCustodianCodes;
}
