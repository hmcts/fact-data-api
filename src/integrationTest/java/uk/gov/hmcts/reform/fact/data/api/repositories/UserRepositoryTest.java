package uk.gov.hmcts.reform.fact.data.api.repositories;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.audit.AuditUserContext;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.UserRole;
import uk.gov.hmcts.reform.fact.data.api.repositories.UserRepository.FavouriteLocationReference;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("User Repository")
@DisplayName("User Repository")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private ServiceCentreRepository serviceCentreRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private AuditUserContext auditUserContext;

    private User user;
    private UUID regionId;

    @BeforeEach
    void setUp() {
        auditUserContext.clear();
        auditUserContext.suppressAudit();
        regionId = regionRepository.save(Region.builder()
            .name("Favourite Test Region")
            .country("England")
            .build()).getId();
        user = userRepository.save(User.builder()
            .email("favourite." + UUID.randomUUID() + "@justice.gov.uk")
            .ssoId(UUID.randomUUID())
            .role(UserRole.ADMIN)
            .build());
    }

    @Test
    void pagesMixedOpenAndClosedLocationsInStableCaseInsensitiveNameOrder() {
        Court zuluCourt = saveCourt("Zulu Favourite Court", true);
        ServiceCentre alphaServiceCentre = saveServiceCentre("alpha Favourite Service Centre", false);
        add(user.getId(), zuluCourt.getId(), SubjectType.COURT);
        add(user.getId(), alphaServiceCentre.getId(), SubjectType.SERVICE_CENTRE);

        Page<FavouriteLocationReference> firstPage = userRepository.findFavouriteLocationsByUserId(
            user.getId(),
            PageRequest.of(0, 1)
        );
        Page<FavouriteLocationReference> secondPage = userRepository.findFavouriteLocationsByUserId(
            user.getId(),
            PageRequest.of(1, 1)
        );

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getContent())
            .extracting(FavouriteLocationReference::getSubjectId)
            .containsExactly(alphaServiceCentre.getId());
        assertThat(secondPage.getContent())
            .extracting(FavouriteLocationReference::getSubjectId)
            .containsExactly(zuluCourt.getId());
    }

    @Test
    void addIsIdempotentAndStatusesAreIsolatedByUser() {
        Court court = saveCourt("Isolated Favourite Court", true);
        final User otherUser = userRepository.save(User.builder()
            .email("other." + UUID.randomUUID() + "@justice.gov.uk")
            .ssoId(UUID.randomUUID())
            .role(UserRole.VIEWER)
            .build());

        add(user.getId(), court.getId(), SubjectType.COURT);
        add(user.getId(), court.getId(), SubjectType.COURT);

        assertThat(userRepository.findExistingFavouriteReferences(user.getId(), List.of(court.getId())))
            .hasSize(1);
        assertThat(userRepository.findExistingFavouriteReferences(otherUser.getId(), List.of(court.getId())))
            .isEmpty();
    }

    @Test
    void removesBothSubjectTypesIdempotentlyFromTheUserArrays() {
        Court court = saveCourt("Removed Favourite Court", true);
        ServiceCentre serviceCentre = saveServiceCentre("Removed Favourite Service Centre", true);
        add(user.getId(), court.getId(), SubjectType.COURT);
        add(user.getId(), serviceCentre.getId(), SubjectType.SERVICE_CENTRE);

        userRepository.removeFavouriteCourt(user.getId(), court.getId());
        userRepository.removeFavouriteCourt(user.getId(), court.getId());
        userRepository.removeFavouriteServiceCentre(user.getId(), serviceCentre.getId());
        userRepository.removeFavouriteServiceCentre(user.getId(), serviceCentre.getId());

        assertThat(userRepository.findExistingFavouriteReferences(
            user.getId(),
            List.of(court.getId(), serviceCentre.getId())
        )).isEmpty();
    }

    @Test
    void cleanupMethodsRemoveLocationsFromEveryUsersFavourites() {
        Court court = saveCourt("Deleted Favourite Court", true);
        ServiceCentre serviceCentre = saveServiceCentre("Deleted Favourite Service Centre", true);
        User otherUser = userRepository.save(User.builder()
            .email("cleanup." + UUID.randomUUID() + "@justice.gov.uk")
            .ssoId(UUID.randomUUID())
            .role(UserRole.VIEWER)
            .build());
        add(user.getId(), court.getId(), SubjectType.COURT);
        add(user.getId(), serviceCentre.getId(), SubjectType.SERVICE_CENTRE);
        add(otherUser.getId(), court.getId(), SubjectType.COURT);
        add(otherUser.getId(), serviceCentre.getId(), SubjectType.SERVICE_CENTRE);

        userRepository.removeCourtFromAllFavourites(court.getId());
        userRepository.removeServiceCentreFromAllFavourites(serviceCentre.getId());

        assertThat(userRepository.findExistingFavouriteReferences(
            user.getId(),
            List.of(court.getId(), serviceCentre.getId())
        )).isEmpty();
        assertThat(userRepository.findExistingFavouriteReferences(
            otherUser.getId(),
            List.of(court.getId(), serviceCentre.getId())
        )).isEmpty();
    }

    private void add(UUID userId, UUID subjectId, SubjectType subjectType) {
        switch (subjectType) {
            case COURT -> userRepository.addFavouriteCourtIfAbsent(userId, subjectId);
            case SERVICE_CENTRE -> userRepository.addFavouriteServiceCentreIfAbsent(userId, subjectId);
        }
    }

    private Court saveCourt(String name, boolean open) {
        return courtRepository.save(Court.builder()
            .name(name)
            .slug(UUID.randomUUID().toString())
            .open(open)
            .regionId(regionId)
            .build());
    }

    private ServiceCentre saveServiceCentre(String name, boolean open) {
        return serviceCentreRepository.save(ServiceCentre.builder()
            .name(name)
            .slug(UUID.randomUUID().toString())
            .open(open)
            .regionId(regionId)
            .build());
    }
}
