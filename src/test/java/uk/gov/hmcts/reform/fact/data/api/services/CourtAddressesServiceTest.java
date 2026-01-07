package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AddressType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAddressRepository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtAddressesServiceTest {

    @Mock
    private CourtAddressRepository courtAddressRepository;

    @Mock
    private CourtService courtService;

    @Mock
    private TypesService typesService;

    @InjectMocks
    private CourtAddressesService courtAddressesService;

    private UUID courtId;
    private UUID addressId;
    private Court court;
    private CourtAddress address;

    @BeforeEach
    void setup() {
        courtId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        court = new Court();
        court.setId(courtId);
        court.setName("Test Court");

        address = CourtAddress.builder()
            .id(addressId)
            .courtId(courtId)
            .addressLine1("123 Test Street")
            .townCity("Test City")
            .postcode("TE1 1ST")
            .addressType(AddressType.VISIT_US)
            .build();
    }

    @Test
    void getAddressesReturnsRecords() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAddressRepository.findByCourtId(courtId)).thenReturn(List.of(address));

        List<CourtAddress> result = courtAddressesService.getAddresses(courtId);

        assertThat(result).containsExactly(address);
        verify(courtAddressRepository).findByCourtId(courtId);
    }

    @Test
    void getAddressReturnsRecord() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAddressRepository.findByIdAndCourtId(addressId, courtId))
            .thenReturn(Optional.of(address));

        CourtAddress result = courtAddressesService.getAddress(courtId, addressId);

        assertThat(result).isEqualTo(address);
    }

    @Test
    void getAddressThrowsNotFoundWhenMissing() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAddressRepository.findByIdAndCourtId(addressId, courtId))
            .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            courtAddressesService.getAddress(courtId, addressId)
        );

        assertThat(exception.getMessage()).contains(addressId.toString());
    }

    @Test
    void createAddressSetsCourtAndValidatesTypes() {
        List<UUID> areaIds = List.of(UUID.randomUUID());
        List<UUID> courtTypeIds = List.of(UUID.randomUUID());

        CourtAddress newAddress = CourtAddress.builder()
            .addressLine1("New Street")
            .townCity("New City")
            .postcode("NE1 2ST")
            .areasOfLaw(areaIds)
            .courtTypes(courtTypeIds)
            .build();

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(typesService.getAllAreasOfLawTypesByIds(areaIds)).thenReturn(List.of(new AreaOfLawType()));
        when(typesService.getAllCourtTypesByIds(courtTypeIds)).thenReturn(List.of(new CourtType()));
        when(courtAddressRepository.save(any(CourtAddress.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CourtAddress result = courtAddressesService.createAddress(courtId, newAddress);

        assertThat(result.getCourtId()).isEqualTo(courtId);
        assertThat(result.getCourt()).isEqualTo(court);
        verify(courtAddressRepository).save(newAddress);
    }

    @Test
    void createAddressAllowsNullAddressLine() {
        List<UUID> areaIds = List.of(UUID.randomUUID());
        List<UUID> courtTypeIds = List.of(UUID.randomUUID());

        CourtAddress newAddress = CourtAddress.builder()
            .addressLine1(null)
            .addressLine2("Suite 2")
            .townCity("Updated City")
            .county("Updated County")
            .postcode("UP1 2ED")
            .addressType(AddressType.VISIT_US)
            .areasOfLaw(areaIds)
            .courtTypes(courtTypeIds)
            .build();

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAddressRepository.save(any(CourtAddress.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CourtAddress result = courtAddressesService.createAddress(courtId, newAddress);

        assertThat(result.getCourtId()).isEqualTo(courtId);
        assertThat(result.getAddressLine1()).isNull();
    }

    @Test
    void createAddressOnlyAddsValidAreasOfLaw() {
        UUID validAreaOfLawId = UUID.randomUUID();
        List<UUID> areasOfLawIds = List.of(UUID.randomUUID(), validAreaOfLawId, UUID.randomUUID());
        CourtAddress newAddress = CourtAddress.builder()
            .addressLine1("Address Line 1")
            .postcode("UP1 2ED")
            .addressType(AddressType.VISIT_US)
            .areasOfLaw(areasOfLawIds)
            .build();

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAddressRepository.save(any(CourtAddress.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(typesService.getAllAreasOfLawTypesByIds(areasOfLawIds))
            .thenReturn(List.of(new AreaOfLawType(){ { setId(validAreaOfLawId); setName("name"); }}));

        CourtAddress result = courtAddressesService.createAddress(courtId, newAddress);

        assertThat(result.getCourtId()).isEqualTo(courtId);
        assertThat((long) result.getAreasOfLaw().size()).isEqualTo(1);
        assertThat(result.getAreasOfLaw().getFirst()).isEqualTo(validAreaOfLawId);
    }

    @Test
    void updateAddressAppliesNewValues() {
        List<UUID> areaIds = List.of(UUID.randomUUID());
        List<UUID> courtTypeIds = List.of(UUID.randomUUID());

        CourtAddress update = CourtAddress.builder()
            .addressLine1("Updated Street")
            .addressLine2("Suite 2")
            .townCity("Updated City")
            .county("Updated County")
            .postcode("UP1 2ED")
            .epimId("12345")
            .addressType(AddressType.VISIT_US)
            .areasOfLaw(areaIds)
            .courtTypes(courtTypeIds)
            .build();

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAddressRepository.findByIdAndCourtId(addressId, courtId))
            .thenReturn(Optional.of(address));
        when(typesService.getAllAreasOfLawTypesByIds(areaIds)).thenReturn(List.of(new AreaOfLawType()));
        when(typesService.getAllCourtTypesByIds(courtTypeIds)).thenReturn(List.of(new CourtType()));
        when(courtAddressRepository.save(address)).thenReturn(address);

        CourtAddress result = courtAddressesService.updateAddress(courtId, addressId, update);

        assertThat(result.getAddressLine1()).isEqualTo("Updated Street");
        assertThat(result.getAddressLine2()).isEqualTo("Suite 2");
        assertThat(result.getTownCity()).isEqualTo("Updated City");
        assertThat(result.getCounty()).isEqualTo("Updated County");
        assertThat(result.getPostcode()).isEqualTo("UP1 2ED");
        assertThat(result.getEpimId()).isEqualTo("12345");
        assertThat(result.getAddressType()).isEqualTo(AddressType.VISIT_US);
    }

    @Test
    void updateAddressThrowsWhenNotFound() {
        CourtAddress update = CourtAddress.builder().build();

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAddressRepository.findByIdAndCourtId(addressId, courtId))
            .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            courtAddressesService.updateAddress(courtId, addressId, update)
        );
    }

    @Test
    void deleteAddressRemovesEntry() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAddressRepository.existsByIdAndCourtId(addressId, courtId)).thenReturn(true);

        courtAddressesService.deleteAddress(courtId, addressId);

        verify(courtAddressRepository).deleteByIdAndCourtId(addressId, courtId);
    }

    @Test
    void deleteAddressThrowsWhenMissing() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtAddressRepository.existsByIdAndCourtId(addressId, courtId)).thenReturn(false);

        assertThrows(
            NotFoundException.class, () ->
                courtAddressesService.deleteAddress(courtId, addressId)
        );
    }
}
