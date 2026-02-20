package uk.gov.hmcts.reform.fact.data.api.entities;

import java.time.LocalTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
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

import uk.gov.hmcts.reform.fact.data.api.audit.AuditableCourtEntityListener;
import uk.gov.hmcts.reform.fact.data.api.entities.types.DayOfTheWeek;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidTimeOrder;
import uk.gov.hmcts.reform.fact.data.api.controllers.CourtController.CourtDetailsView;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@EntityListeners(AuditableCourtEntityListener.class)
@ValidTimeOrder(start = "openingHour", end = "closingHour")
@JsonView(CourtDetailsView.class)
@Table(name = "court_opening_hours")
public class CourtOpeningHours implements AuditableCourtEntity {

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

    @Schema(description = "The ID of the associated Opening Hour Type")
    @NotNull
    @Column(name = "opening_hour_type")
    private UUID openingHourTypeId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opening_hour_type", insertable = false, updatable = false)
    private OpeningHourType openingHourType;

    @Transient
    @JsonIgnore
    private OpeningHourType openingHourTypeDetails;

    @JsonView(CourtDetailsView.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("openingHourType")
    public OpeningHourType getOpeningHourTypeForView() {
        return openingHourTypeDetails;
    }

    @Schema(description = "Day of the week or every day.")
    @NotNull
    @Enumerated(EnumType.STRING)
    private DayOfTheWeek dayOfWeek;

    @Schema(description = "Opening hour")
    @NotNull
    private LocalTime openingHour;

    @Schema(description = "Closing hour")
    @NotNull
    private LocalTime closingHour;
}
