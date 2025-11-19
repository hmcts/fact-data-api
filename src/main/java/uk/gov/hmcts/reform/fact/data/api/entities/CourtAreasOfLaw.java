package uk.gov.hmcts.reform.fact.data.api.entities;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "court_areas_of_law")
public class CourtAreasOfLaw {

    @Schema(
        description = "The internal ID - assigned by the server during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Schema(description = "The ID of the associated Court", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Column(name = "court_id")
    private UUID courtId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    private Court court;

    @Schema(description = "the list of associated Areas of Law")
    @Type(ListArrayType.class)
    @Column(columnDefinition = "uuid[]")
    private List<UUID> areasOfLaw;
}
