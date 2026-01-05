package uk.gov.hmcts.reform.fact.data.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
public class CourtProfessionalInformationDetailsDto {

    @Schema(description = "Primary professional information for the court", required = true)
    @Valid
    @NotNull
    private ProfessionalInformationDto professionalInformation;

    @Schema(description = "Supplementary court codes for the court")
    @Valid
    private CourtCodesDto codes;

    @Schema(description = "DX codes available for the court")
    @Valid
    @Builder.Default
    private List<@Valid @NotNull CourtDxCodeDto> dxCodes = new ArrayList<>();

    @Schema(description = "Fax numbers associated with the court")
    @Valid
    @Builder.Default
    private List<@Valid @NotNull CourtFaxDto> faxNumbers = new ArrayList<>();
}
