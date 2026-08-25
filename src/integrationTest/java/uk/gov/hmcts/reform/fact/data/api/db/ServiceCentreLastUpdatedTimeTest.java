package uk.gov.hmcts.reform.fact.data.api.db;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.fact.data.api.audit.AuditUserContext;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreContactDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AddressType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreAddressRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreRepository;
import uk.gov.hmcts.reform.fact.data.api.services.OsService;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreAddressService;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreAreasOfLawService;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreContactDetailsService;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreService;
import uk.gov.hmcts.reform.fact.data.api.services.TypesService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Feature("Service Centre Change Updates")
@DisplayName("Service Centre Change Updates")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class ServiceCentreLastUpdatedTimeTest {

    @Autowired
    private ServiceCentreService serviceCentreService;

    @Autowired
    private ServiceCentreAddressService serviceCentreAddressService;

    @Autowired
    private ServiceCentreContactDetailsService serviceCentreContactDetailsService;

    @Autowired
    private ServiceCentreAreasOfLawService serviceCentreAreasOfLawService;

    @Autowired
    private TypesService typesService;

    @Autowired
    private ServiceCentreRepository serviceCentreRepository;

    @Autowired
    private ServiceCentreAddressRepository serviceCentreAddressRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private AuditUserContext auditUserContext;

    @MockitoBean
    private OsService osService;

    private ServiceCentre monitoredServiceCentre;
    private ServiceCentre controlServiceCentre;
    private UUID regionId;

    @BeforeEach
    void setUp() {
        auditUserContext.clear();
        auditUserContext.setUserId(UUID.randomUUID());
        when(osService.getOsAddressByFullPostcode(anyString())).thenReturn(null);
        regionId = regionRepository.findAll().stream()
            .findFirst()
            .map(Region::getId)
            .orElseGet(() -> regionRepository.save(Region.builder()
                .name("Service Centre Last Updated Region")
                .country("England")
                .build()).getId());

        monitoredServiceCentre = createServiceCentre("Monitored Service Centre ");
        controlServiceCentre = createServiceCentre("Control Service Centre ");
    }

    @AfterEach
    void tearDown() {
        serviceCentreAddressRepository.deleteAll();
        serviceCentreRepository.deleteAll();
        auditUserContext.clear();
    }

    @Test
    @DisplayName("Ensure service centre last_updated_at is updated on address update")
    void updatingAddressUpdatesServiceCentreLastUpdatedTimestamp() throws InterruptedException {
        ServiceCentreAddress address = ServiceCentreAddress.builder()
            .addressLine1("1 Test Street")
            .townCity("London")
            .postcode("SW1A 1AA")
            .addressType(AddressType.VISIT_US)
            .build();

        ServiceCentreAddress createdAddress = serviceCentreAddressService
            .createAddress(monitoredServiceCentre.getId(), address);

        ServiceCentre refreshedMonitored = findServiceCentre(monitoredServiceCentre.getId());
        ServiceCentre refreshedControl = findServiceCentre(controlServiceCentre.getId());
        final ZonedDateTime monitoredBeforeUpdate = refreshedMonitored.getLastUpdatedAt();
        final ZonedDateTime controlBeforeUpdate = refreshedControl.getLastUpdatedAt();

        Thread.sleep(1);

        createdAddress.setAddressLine1("2 Updated Street");
        createdAddress.setPostcode("EC1A 1BB");
        serviceCentreAddressService.updateAddress(
            monitoredServiceCentre.getId(),
            createdAddress.getId(),
            createdAddress
        );

        refreshedMonitored = findServiceCentre(monitoredServiceCentre.getId());
        refreshedControl = findServiceCentre(controlServiceCentre.getId());

        assertThat(refreshedMonitored.getLastUpdatedAt()).isAfter(monitoredBeforeUpdate);
        assertThat(refreshedControl.getLastUpdatedAt()).isEqualTo(controlBeforeUpdate);
    }

    @Test
    @DisplayName("Ensure service centre last_updated_at is updated on contact detail update")
    void updatingContactDetailUpdatesServiceCentreLastUpdatedTimestamp() throws InterruptedException {
        ServiceCentreContactDetails contactDetails = ServiceCentreContactDetails.builder()
            .email("first@example.com")
            .phoneNumber("02079460000")
            .build();

        ServiceCentreContactDetails createdContactDetails = serviceCentreContactDetailsService
            .createContactDetail(monitoredServiceCentre.getId(), contactDetails);

        ServiceCentre refreshedMonitored = findServiceCentre(monitoredServiceCentre.getId());
        ServiceCentre refreshedControl = findServiceCentre(controlServiceCentre.getId());
        final ZonedDateTime monitoredBeforeUpdate = refreshedMonitored.getLastUpdatedAt();
        final ZonedDateTime controlBeforeUpdate = refreshedControl.getLastUpdatedAt();

        Thread.sleep(1);

        createdContactDetails.setEmail("updated@example.com");
        createdContactDetails.setPhoneNumber("02079460001");
        serviceCentreContactDetailsService.updateContactDetail(
            monitoredServiceCentre.getId(),
            createdContactDetails.getId(),
            createdContactDetails
        );

        refreshedMonitored = findServiceCentre(monitoredServiceCentre.getId());
        refreshedControl = findServiceCentre(controlServiceCentre.getId());

        assertThat(refreshedMonitored.getLastUpdatedAt()).isAfter(monitoredBeforeUpdate);
        assertThat(refreshedControl.getLastUpdatedAt()).isEqualTo(controlBeforeUpdate);
    }

    @Test
    @DisplayName("Ensure service centre last_updated_at is updated on areas of law update")
    void updatingAreasOfLawUpdatesServiceCentreLastUpdatedTimestamp() throws InterruptedException {
        List<AreaOfLawType> areaOfLawTypes = typesService.getAreaOfLawTypes();
        assertThat(areaOfLawTypes).isNotEmpty();

        final UUID firstAreaId = areaOfLawTypes.getFirst().getId();
        final UUID secondAreaId = areaOfLawTypes.size() > 1 ? areaOfLawTypes.get(1).getId() : firstAreaId;

        final ServiceCentreAreasOfLaw initialAreas = ServiceCentreAreasOfLaw.builder()
            .areasOfLaw(List.of(firstAreaId))
            .build();
        serviceCentreAreasOfLawService.setServiceCentreAreasOfLaw(monitoredServiceCentre.getId(), initialAreas);

        ServiceCentre refreshedMonitored = findServiceCentre(monitoredServiceCentre.getId());
        ServiceCentre refreshedControl = findServiceCentre(controlServiceCentre.getId());
        final ZonedDateTime monitoredBeforeUpdate = refreshedMonitored.getLastUpdatedAt();
        final ZonedDateTime controlBeforeUpdate = refreshedControl.getLastUpdatedAt();

        Thread.sleep(1);

        ServiceCentreAreasOfLaw updatedAreas = ServiceCentreAreasOfLaw.builder()
            .areasOfLaw(List.of(secondAreaId))
            .build();
        serviceCentreAreasOfLawService.setServiceCentreAreasOfLaw(monitoredServiceCentre.getId(), updatedAreas);

        refreshedMonitored = findServiceCentre(monitoredServiceCentre.getId());
        refreshedControl = findServiceCentre(controlServiceCentre.getId());

        assertThat(refreshedMonitored.getLastUpdatedAt()).isAfter(monitoredBeforeUpdate);
        assertThat(refreshedControl.getLastUpdatedAt()).isEqualTo(controlBeforeUpdate);
    }

    @Test
    @DisplayName("Ensure failed child update does not change service centre last_updated_at")
    void failedContactDetailUpdateDoesNotChangeServiceCentreLastUpdatedTimestamp() {
        ServiceCentreContactDetails contactDetails = ServiceCentreContactDetails.builder()
            .email("first@example.com")
            .phoneNumber("02079460000")
            .build();

        ServiceCentreContactDetails createdContactDetails = serviceCentreContactDetailsService
            .createContactDetail(monitoredServiceCentre.getId(), contactDetails);

        ServiceCentre refreshedMonitored = findServiceCentre(monitoredServiceCentre.getId());
        ServiceCentre refreshedControl = findServiceCentre(controlServiceCentre.getId());
        final ZonedDateTime monitoredBeforeFailure = refreshedMonitored.getLastUpdatedAt();
        final ZonedDateTime controlBeforeFailure = refreshedControl.getLastUpdatedAt();

        createdContactDetails.setServiceCentreContactDescriptionId(UUID.randomUUID());

        assertThatThrownBy(() -> serviceCentreContactDetailsService.updateContactDetail(
            monitoredServiceCentre.getId(),
            createdContactDetails.getId(),
            createdContactDetails
        )).isInstanceOf(NotFoundException.class);

        refreshedMonitored = findServiceCentre(monitoredServiceCentre.getId());
        refreshedControl = findServiceCentre(controlServiceCentre.getId());

        assertThat(refreshedMonitored.getLastUpdatedAt()).isEqualTo(monitoredBeforeFailure);
        assertThat(refreshedControl.getLastUpdatedAt()).isEqualTo(controlBeforeFailure);
    }

    private ServiceCentre createServiceCentre(final String namePrefix) {
        ServiceCentre request = ServiceCentre.builder()
            .name(namePrefix)
            .serviceAreaIds(List.of())
            .regionId(regionId)
            .build();

        return serviceCentreService.createServiceCentre(request);
    }

    private ServiceCentre findServiceCentre(final UUID serviceCentreId) {
        return serviceCentreRepository.findById(serviceCentreId).orElseThrow();
    }
}


