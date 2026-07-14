package uk.gov.hmcts.reform.fact.data.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.fact.data.api.audit.AuditableCourtEntityListener;
import uk.gov.hmcts.reform.fact.data.api.controllers.ServiceCentreController.ServiceCentreDetailsView;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@EntityListeners(AuditableCourtEntityListener.class)
@JsonView(ServiceCentreDetailsView.class)
@Table(name = "service_centre_areas_of_law")
public class ServiceCentreAreasOfLaw implements AuditableEntity {

    @Schema(
        description = "The internal ID - assigned by the server during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Schema(description = "The ID of the associated Service Centre", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Column(name = "service_centre_id")
    private UUID serviceCentreId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_centre_id", insertable = false, updatable = false)
    private ServiceCentre serviceCentre;

    @Schema(description = "The list of associated Areas of Law")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "uuid[]")
    private List<UUID> areasOfLaw;

    @Transient
    @JsonIgnore
    private List<AreaOfLawType> areasOfLawDetails;

    @JsonView(ServiceCentreDetailsView.class)
    @JsonProperty("areasOfLaw")
    public List<?> getAreasOfLawForView() {
        return areasOfLawDetails != null ? areasOfLawDetails : areasOfLaw;
    }

    @JsonIgnore
    public List<UUID> getAreasOfLaw() {
        return areasOfLaw;
    }

    @JsonSetter("serviceCentreId")
    public void setServiceCentreId(UUID serviceCentreId) {
        this.serviceCentreId = serviceCentreId;
    }

    @JsonSetter("areasOfLaw")
    public void setAreasOfLaw(List<?> areasOfLaw) {
        this.areasOfLaw = areasOfLaw == null
            ? null
            : areasOfLaw.stream().map(ServiceCentreAreasOfLaw::extractAreaOfLawId).toList();
    }

    private static UUID extractAreaOfLawId(Object areaOfLaw) {
        if (areaOfLaw instanceof UUID id) {
            return id;
        }
        if (areaOfLaw instanceof String id) {
            return UUID.fromString(id);
        }
        if (areaOfLaw instanceof AreaOfLawType areaOfLawType) {
            return areaOfLawType.getId();
        }
        if (areaOfLaw instanceof Map<?, ?> areaOfLawType) {
            Object id = areaOfLawType.get("id");
            return id instanceof UUID uuid ? uuid : UUID.fromString(String.valueOf(id));
        }
        return UUID.fromString(String.valueOf(areaOfLaw));
    }

    @Override
    public UUID getAuditSubjectId() {
        return serviceCentreId;
    }

    @Override
    public SubjectType getAuditSubjectType() {
        return SubjectType.SERVICE_CENTRE;
    }
}
