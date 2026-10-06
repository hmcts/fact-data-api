package uk.gov.hmcts.reform.fact.data.api.entities.types;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.controllers.CourtController.CourtDetailsView;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
@JsonView(CourtDetailsView.class)
public class FoodAndDrinkOptions {

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
}
