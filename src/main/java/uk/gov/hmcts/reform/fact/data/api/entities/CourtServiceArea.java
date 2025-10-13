package uk.gov.hmcts.reform.fact.data.api.entities;

import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;

import java.util.List;
import java.util.UUID;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Type;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "court_service_areas", schema = "public")
public class CourtServiceArea extends BaseCourtEntity {

    @Schema(description = "The Service Area IDs")
    @Type(ListArrayType.class)
    @Column(columnDefinition = "uuid[]")
    private List<UUID> serviceAreaId;

    @Schema(description = "The catchment type")
    // conversion is handled by a custom Converter implementation
    private CatchmentType catchmentType;

}
