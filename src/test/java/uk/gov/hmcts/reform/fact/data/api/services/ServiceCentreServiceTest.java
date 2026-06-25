package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreDetailsRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceCentreServiceTest {

    @Mock
    private ServiceCentreRepository serviceCentreRepository;

    @Mock
    private ServiceCentreDetailsRepository serviceCentreDetailsRepository;

    @Mock
    private ServiceAreaRepository serviceAreaRepository;

    @InjectMocks
    private ServiceCentreService serviceCentreService;

    @Test
    void getServiceCentreByIdReturnsServiceCentreWhenFound() {
        UUID serviceCentreId = UUID.randomUUID();
        ServiceCentre serviceCentre = ServiceCentre.builder().id(serviceCentreId).name("Test Service Centre").build();

        when(serviceCentreRepository.findById(serviceCentreId)).thenReturn(Optional.of(serviceCentre));

        ServiceCentre result = serviceCentreService.getServiceCentreById(serviceCentreId);

        assertThat(result).isEqualTo(serviceCentre);
    }

    @Test
    void getServiceCentreByIdThrowsNotFoundWhenMissing() {
        UUID serviceCentreId = UUID.randomUUID();
        when(serviceCentreRepository.findById(serviceCentreId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
            NotFoundException.class,
            () -> serviceCentreService.getServiceCentreById(serviceCentreId)
        );

        assertThat(exception.getMessage()).isEqualTo("Service centre not found, ID: " + serviceCentreId);
    }

    @Test
    void getServiceCentreDetailsByIdReturnsDetailsWhenFound() {
        UUID serviceCentreId = UUID.randomUUID();
        ServiceCentreDetails serviceCentreDetails = ServiceCentreDetails.builder()
            .id(serviceCentreId)
            .name("Test Service Centre")
            .build();

        when(serviceCentreDetailsRepository.findById(serviceCentreId)).thenReturn(Optional.of(serviceCentreDetails));

        ServiceCentreDetails result = serviceCentreService.getServiceCentreDetailsById(serviceCentreId);

        assertThat(result).isEqualTo(serviceCentreDetails);
    }

    @Test
    void getServiceCentreDetailsByIdThrowsNotFoundWhenMissing() {
        UUID serviceCentreId = UUID.randomUUID();
        when(serviceCentreDetailsRepository.findById(serviceCentreId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
            NotFoundException.class,
            () -> serviceCentreService.getServiceCentreDetailsById(serviceCentreId)
        );

        assertThat(exception.getMessage()).isEqualTo("Service centre not found, ID: " + serviceCentreId);
    }

    @Test
    void getServiceCentreDetailsBySlugReturnsDetailsWhenFound() {
        ServiceCentreDetails serviceCentreDetails = ServiceCentreDetails.builder()
            .slug("test-service-centre")
            .name("Test Service Centre")
            .build();

        when(serviceCentreDetailsRepository.findBySlug("test-service-centre"))
            .thenReturn(Optional.of(serviceCentreDetails));

        ServiceCentreDetails result = serviceCentreService.getServiceCentreDetailsBySlug("test-service-centre");

        assertThat(result).isEqualTo(serviceCentreDetails);
    }

    @Test
    void getServiceCentreDetailsBySlugThrowsNotFoundWhenMissing() {
        when(serviceCentreDetailsRepository.findBySlug("missing-service-centre")).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
            NotFoundException.class,
            () -> serviceCentreService.getServiceCentreDetailsBySlug("missing-service-centre")
        );

        assertThat(exception.getMessage()).isEqualTo("Service centre not found, slug: missing-service-centre");
    }

    @Test
    void getServiceCentreByNameReturnsServiceCentre() {
        ServiceCentre serviceCentre = ServiceCentre.builder().name("Bulk Scan Centre").build();

        when(serviceCentreRepository.findByName("Bulk Scan Centre")).thenReturn(Optional.of(serviceCentre));

        assertThat(serviceCentreService.getServiceCentreByName("Bulk Scan Centre")).isEqualTo(serviceCentre);
    }

    @Test
    void createServiceCentreGeneratesUniqueSlugStartsClosedAndNormalisesServiceAreas() {
        UUID suppliedServiceAreaId = UUID.randomUUID();
        UUID validServiceAreaId = UUID.randomUUID();
        ServiceArea serviceArea = ServiceArea.builder().id(validServiceAreaId).build();
        ServiceCentre request = ServiceCentre.builder()
            .name("Bulk Scan Centre")
            .open(true)
            .serviceAreaIds(List.of(suppliedServiceAreaId))
            .catchmentType(CatchmentType.REGIONAL)
            .build();

        when(serviceAreaRepository.findAllById(List.of(suppliedServiceAreaId))).thenReturn(List.of(serviceArea));
        when(serviceCentreRepository.existsBySlug("bulk-scan-centre")).thenReturn(true);
        when(serviceCentreRepository.existsBySlug("bulk-scan-centre-1")).thenReturn(false);
        when(serviceCentreRepository.save(any(ServiceCentre.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceCentre result = serviceCentreService.createServiceCentre(request);

        assertThat(result.getSlug()).isEqualTo("bulk-scan-centre-1");
        assertThat(result.getOpen()).isFalse();
        assertThat(result.getServiceAreaIds()).containsExactly(validServiceAreaId);
        assertThat(result.getCatchmentType()).isEqualTo(CatchmentType.REGIONAL);
    }

    @Test
    void updateServiceCentreRegeneratesSlugWhenNameChanges() {
        UUID serviceCentreId = UUID.randomUUID();
        ServiceCentre existing = ServiceCentre.builder()
            .id(serviceCentreId)
            .name("Old Service Centre")
            .slug("old-service-centre")
            .open(false)
            .build();
        ServiceCentre request = ServiceCentre.builder()
            .name("New Service Centre")
            .open(true)
            .warningNotice("Warning")
            .serviceAreaIds(List.of())
            .catchmentType(CatchmentType.LOCAL)
            .build();

        when(serviceCentreRepository.findById(serviceCentreId)).thenReturn(Optional.of(existing));
        when(serviceCentreRepository.existsBySlug("new-service-centre")).thenReturn(false);
        when(serviceAreaRepository.findAllById(List.of())).thenReturn(List.of());
        when(serviceCentreRepository.save(existing)).thenReturn(existing);

        ServiceCentre result = serviceCentreService.updateServiceCentre(serviceCentreId, request);

        assertThat(result.getName()).isEqualTo("New Service Centre");
        assertThat(result.getSlug()).isEqualTo("new-service-centre");
        assertThat(result.getOpen()).isTrue();
        assertThat(result.getWarningNotice()).isEqualTo("Warning");
        assertThat(result.getCatchmentType()).isEqualTo(CatchmentType.LOCAL);
    }

    @Test
    void deleteServiceCentresByNamePrefixDeletesMatchingRowsForTestingSupport() {
        ServiceCentre first = ServiceCentre.builder().id(UUID.randomUUID()).name("SC Delete One").build();
        ServiceCentre second = ServiceCentre.builder().id(UUID.randomUUID()).name("SC Delete Two").build();

        List<ServiceCentre> serviceCentres = List.of(first, second);
        when(serviceCentreRepository.findByNameStartingWithIgnoreCase("SC Delete")).thenReturn(serviceCentres);

        long deleted = serviceCentreService.deleteServiceCentresByNamePrefix("SC Delete");

        assertThat(deleted).isEqualTo(2);
        verify(serviceCentreRepository).deleteAllInBatch(serviceCentres);
    }
}
