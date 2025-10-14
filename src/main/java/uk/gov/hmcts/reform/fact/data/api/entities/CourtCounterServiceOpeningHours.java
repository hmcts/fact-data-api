package uk.gov.hmcts.reform.fact.data.api.entities;

import java.time.LocalTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@Table(name = "court_counter_service_opening_hours")
public class CourtCounterServiceOpeningHours {

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

    @Schema(description = "Counter service availability status")
    @NotNull
    private Boolean counterService;

    @Schema(description = "'Assist with Forms' service availability status")
    @NotNull
    private Boolean assistWithForms;

    @Schema(description = "'Assist with Documents' service availability status")
    @NotNull
    private Boolean assistWithDocuments;

    @Schema(description = "'Assist with Support' availability status")
    @NotNull
    private Boolean assistWithSupport;

    @Schema(description = "Arranged appointment requirement status")
    @NotNull
    private Boolean appointmentNeeded;

    @Schema(description = "Appointment arrangement contact details")
    @Size(max = 255, message = "Appointment contact details can be at most {max} characters")
    private String appointmentContact;

    @Schema(description = "Day of the week")
    private Integer dayOfWeek;

    @Schema(description = "Opening hour")
    private LocalTime openingHour;

    @Schema(description = "Closing hour")
    private LocalTime closingHour;

}
