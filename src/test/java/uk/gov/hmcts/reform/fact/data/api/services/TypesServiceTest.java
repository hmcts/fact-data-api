package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.*;
import uk.gov.hmcts.reform.fact.data.api.entities.types.DayOfTheWeek;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.*;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TypesServiceTest {

    @Mock
    private AreaOfLawTypeRepository areaOfLawTypeRepository;

    @Mock
    private CourtTypeRepository courtTypeRepository;

    @Mock
    private OpeningHoursTypeRepository openingHoursTypeRepository;

    @Mock
    private ContactDescriptionTypeRepository contactDescriptionTypeRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private ServiceAreaRepository serviceAreaRepository;

    @InjectMocks
    private TypesService typesService;

    private List<AreaOfLawType> areaOfLawTypes;

    @BeforeEach
    void setup() {
        areaOfLawTypes = List.of(
            AreaOfLawType.builder()
                .id(UUID.randomUUID())
                .name("name")
                .build());
    }

    @Test
    void getAreasOfLawTypesReturnsAreasOfLawTypesWhenFound() {
        when(areaOfLawTypeRepository.findAll()).thenReturn(areaOfLawTypes);

        List<AreaOfLawType> result = typesService.getAreaOfLawTypes();

        assertThat(result).isEqualTo(areaOfLawTypes);
    }


}

