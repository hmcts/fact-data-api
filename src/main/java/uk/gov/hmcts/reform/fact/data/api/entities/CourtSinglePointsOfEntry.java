package uk.gov.hmcts.reform.fact.data.api.entities;

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
@Table(name = "court_single_points_of_entry")
public class CourtSinglePointsOfEntry extends BaseCourtEntity {

    @Schema(description = "The Area of Law Type IDs")
    @Type(ListArrayType.class)
    @Column(columnDefinition = "uuid[]")
    private List<UUID> areasOfLaw;

}
