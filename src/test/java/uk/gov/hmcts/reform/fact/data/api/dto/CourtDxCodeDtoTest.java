package uk.gov.hmcts.reform.fact.data.api.dto;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDxCode;

import static org.assertj.core.api.Assertions.assertThat;

class CourtDxCodeDtoTest {

    @Test
    void explanationAndWelshExplanationMustBeProvidedTogether() {
        CourtDxCodeDto valid = CourtDxCodeDto.builder()
            .dxCode("DX 123")
            .explanation("Use this code")
            .explanationCy("Defnyddiwch y cod hwn")
            .build();

        CourtDxCodeDto invalid = CourtDxCodeDto.builder()
            .dxCode("DX 123")
            .explanation("Use this code")
            .explanationCy(null)
            .build();

        assertThat(valid.isExplanationCyPresentWhenExplanationProvided()).isTrue();
        assertThat(invalid.isExplanationCyPresentWhenExplanationProvided()).isFalse();
    }

    @Test
    void fromEntityMapsFields() {
        CourtDxCode entity = CourtDxCode.builder()
            .dxCode("DX 999")
            .explanation("Main desk")
            .explanationCy("Prif ddesg")
            .build();

        CourtDxCodeDto dto = CourtDxCodeDto.fromEntity(entity);

        assertThat(dto.getDxCode()).isEqualTo("DX 999");
        assertThat(dto.getExplanation()).isEqualTo("Main desk");
        assertThat(dto.getExplanationCy()).isEqualTo("Prif ddesg");
    }
}

