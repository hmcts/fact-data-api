package uk.gov.hmcts.reform.fact.data.api.entities;

import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "court_counter_service_opening_hours")
public class CourtCounterServiceOpeningHours extends IdBasedEntityWithCourt {

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
