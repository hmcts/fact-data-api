package uk.gov.hmcts.reform.fact.data.api.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "court_facilities")
public class CourtFacility extends BaseCourtEntity {

    @Schema(description = "Parking availability status")
    @NotNull
    private Boolean parking;

    @Schema(description = "Free water dispenser availability status")
    @NotNull
    private Boolean freeWaterDispensers;

    @Schema(description = "Snack vending machine availability status")
    @NotNull
    private Boolean snackVendingMachines;

    @Schema(description = "Drink vending machine availability status")
    @NotNull
    private Boolean drinkVendingMachines;

    @Schema(description = "Cafeteria availability status")
    @NotNull
    private Boolean cafeteria;

    @Schema(description = "Waiting area availability status")
    @NotNull
    private Boolean waitingArea;

    @Schema(description = "Child-specific waiting area availability status")
    @NotNull
    private Boolean waitingAreaChildren;

    @Schema(description = "Quiet room availability status")
    @NotNull
    private Boolean quietRoom;

    @Schema(description = "Baby changing facilities availability status")
    @NotNull
    private Boolean babyChanging;

    @Schema(description = "WiFi availability status")
    @NotNull
    private Boolean wifi;

}
