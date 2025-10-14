package uk.gov.hmcts.reform.fact.data.api.entities;

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
@Table(name = "court_facilities")
public class CourtFacility {

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
