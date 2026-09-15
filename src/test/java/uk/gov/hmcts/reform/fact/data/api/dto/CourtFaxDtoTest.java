package uk.gov.hmcts.reform.fact.data.api.dto;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFax;

import static org.assertj.core.api.Assertions.assertThat;

class CourtFaxDtoTest {

    @Test
    void descriptionAndWelshDescriptionMustBeProvidedTogether() {
        CourtFaxDto valid = CourtFaxDto.builder()
            .faxNumber("01234 567890")
            .description("Main line")
            .descriptionCy("Prif linell")
            .build();

        CourtFaxDto invalid = CourtFaxDto.builder()
            .faxNumber("01234 567890")
            .description("Main line")
            .descriptionCy(null)
            .build();

        assertThat(valid.isDescriptionCyPresentWhenDescriptionProvided()).isTrue();
        assertThat(invalid.isDescriptionCyPresentWhenDescriptionProvided()).isFalse();
    }

    @Test
    void fromEntityMapsFields() {
        CourtFax entity = CourtFax.builder()
            .faxNumber("020 7946 0991")
            .description("Civil desk")
            .descriptionCy("Desg sifil")
            .build();

        CourtFaxDto dto = CourtFaxDto.fromEntity(entity);

        assertThat(dto.getFaxNumber()).isEqualTo("020 7946 0991");
        assertThat(dto.getDescription()).isEqualTo("Civil desk");
        assertThat(dto.getDescriptionCy()).isEqualTo("Desg sifil");
    }
}

