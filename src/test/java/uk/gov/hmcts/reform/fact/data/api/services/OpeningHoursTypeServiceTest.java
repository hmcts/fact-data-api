package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.OpeningHoursTypeRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpeningHoursTypeServiceTest {

    @Mock
    private OpeningHoursTypeRepository openingHoursTypeRepository;

    @InjectMocks
    private OpeningHoursTypeService openingHoursTypeService;

    @Test
    void getOpeningHourTypeByIdReturnsOpeningHourTypeWhenFound() {
        UUID id = UUID.randomUUID();
        OpeningHourType openingHourType = OpeningHourType.builder()
            .id(id)
            .name("Public counter")
            .nameCy("Cownter cyhoeddus")
            .build();

        when(openingHoursTypeRepository.findById(id)).thenReturn(Optional.of(openingHourType));

        OpeningHourType result = openingHoursTypeService.getOpeningHourTypeById(id);

        assertThat(result).isEqualTo(openingHourType);
    }

    @Test
    void getOpeningHourTypeByIdThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(openingHoursTypeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> openingHoursTypeService.getOpeningHourTypeById(id))
            .isInstanceOf(CourtResourceNotFoundException.class)
            .hasMessage("Opening hour type not found, ID: " + id);
    }
}

