package uk.gov.hmcts.reform.fact.data.api.entities.types;

import java.io.Serializable;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidTimeOrder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidTimeOrder(start = "openingTime", end = "closingTime")
public class OpeningTimesDetail implements Serializable {

    @Schema(description = "Day of the week or every day.")
    @NotNull
    private DayOfTheWeek dayOfWeek;

    @Schema(description = "Opening time")
    @NotNull
    private LocalTime openingTime;

    @Schema(description = "Closing time")
    @NotNull
    private LocalTime closingTime;
}
