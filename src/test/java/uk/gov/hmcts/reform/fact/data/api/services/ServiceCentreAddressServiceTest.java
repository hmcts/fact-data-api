package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AddressType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsDpa;
import uk.gov.hmcts.reform.fact.data.api.os.OsResult;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreAddressRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceCentreAddressServiceTest {

    @Mock
    private ServiceCentreAddressRepository serviceCentreAddressRepository;

    @Mock
    private ServiceCentreService serviceCentreService;

    @Mock
    private OsService osService;

    @InjectMocks
    private ServiceCentreAddressService serviceCentreAddressService;

    private UUID serviceCentreId;
    private UUID addressId;
    private ServiceCentre serviceCentre;
    private ServiceCentreAddress address;

    @BeforeEach
    void setup() {
        serviceCentreId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        serviceCentre = ServiceCentre.builder().id(serviceCentreId).name("Test Service Centre").build();
        address = ServiceCentreAddress.builder()
            .id(addressId)
            .serviceCentreId(serviceCentreId)
            .addressLine1("1 Test Street")
            .postcode("TE1 1ST")
            .addressType(AddressType.VISIT_US)
            .build();

        OsDpa osDpa = OsDpa.builder().lat(51.5).lng(-0.1).build();
        OsResult osResult = OsResult.builder().dpa(osDpa).build();
        OsData osData = OsData.builder().results(List.of(osResult)).build();
        lenient().when(osService.getOsAddressByFullPostcode(anyString())).thenReturn(osData);
    }

    @Test
    void getAddressesReturnsParentScopedRecords() {
        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(serviceCentreAddressRepository.findByServiceCentreId(serviceCentreId)).thenReturn(List.of(address));

        assertThat(serviceCentreAddressService.getAddresses(serviceCentreId)).containsExactly(address);
    }

    @Test
    void getAddressThrowsNotFoundWhenMissing() {
        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(serviceCentreAddressRepository.findByIdAndServiceCentreId(addressId, serviceCentreId))
            .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
            NotFoundException.class,
            () -> serviceCentreAddressService.getAddress(serviceCentreId, addressId)
        );

        assertThat(exception.getMessage()).contains(addressId.toString());
    }

    @Test
    void createAddressSetsParentAndGeocodesPostcode() {
        ServiceCentreAddress request = ServiceCentreAddress.builder()
            .addressLine1("2 Test Street")
            .postcode("TE1 1ST")
            .build();

        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(serviceCentreAddressRepository.save(any(ServiceCentreAddress.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceCentreAddress result = serviceCentreAddressService.createAddress(serviceCentreId, request);

        assertThat(result.getServiceCentreId()).isEqualTo(serviceCentreId);
        assertThat(result.getServiceCentre()).isEqualTo(serviceCentre);
        assertThat(result.getLat()).isEqualByComparingTo(BigDecimal.valueOf(51.5));
        assertThat(result.getLon()).isEqualByComparingTo(BigDecimal.valueOf(-0.1));
    }

    @Test
    void updateAddressAppliesNewValues() {
        ServiceCentreAddress request = ServiceCentreAddress.builder()
            .addressLine1("Updated Street")
            .addressLine2("Suite 1")
            .townCity("Updated City")
            .county("Updated County")
            .postcode("UP1 2ED")
            .addressType(AddressType.VISIT_OR_CONTACT_US)
            .build();

        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(serviceCentreAddressRepository.findByIdAndServiceCentreId(addressId, serviceCentreId))
            .thenReturn(Optional.of(address));
        when(serviceCentreAddressRepository.save(address)).thenReturn(address);

        ServiceCentreAddress result = serviceCentreAddressService.updateAddress(serviceCentreId, addressId, request);

        assertThat(result.getAddressLine1()).isEqualTo("Updated Street");
        assertThat(result.getAddressType()).isEqualTo(AddressType.VISIT_OR_CONTACT_US);
    }

    @Test
    void deleteAddressDeletesParentScopedRecord() {
        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(serviceCentreAddressRepository.findByIdAndServiceCentreId(addressId, serviceCentreId))
            .thenReturn(Optional.of(address));

        serviceCentreAddressService.deleteAddress(serviceCentreId, addressId);

        verify(serviceCentreAddressRepository).deleteByIdAndServiceCentreId(addressId, serviceCentreId);
    }
}
