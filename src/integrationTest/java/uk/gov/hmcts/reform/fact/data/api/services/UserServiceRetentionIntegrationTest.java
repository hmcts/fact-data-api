package uk.gov.hmcts.reform.fact.data.api.services;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.audit.AuditUserContext;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPhoto;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.UserRole;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtPhotoRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.UserRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("User Service")
@DisplayName("User Service Retention")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceRetentionIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private CourtPhotoRepository courtPhotoRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private AuditUserContext auditUserContext;

    @BeforeEach
    void setUp() {
        auditUserContext.clear();
        auditUserContext.suppressAudit();
    }

    @Test
    void deleteInactiveUsersRetainsCourtPhotoAndNullsUpdatedByUserId() {
        final Region region = regionRepository.save(Region.builder()
            .name("Retention Test Region")
            .country("England")
            .build());

        final User inactiveUser = userRepository.save(User.builder()
            .email("inactive." + UUID.randomUUID() + "@justice.gov.uk")
            .ssoId(UUID.randomUUID())
            .role(UserRole.ADMIN)
            .lastLogin(ZonedDateTime.now().minusDays(500))
            .build());

        userRepository.save(User.builder()
            .email("active." + UUID.randomUUID() + "@justice.gov.uk")
            .ssoId(UUID.randomUUID())
            .role(UserRole.VIEWER)
            .lastLogin(ZonedDateTime.now().minusDays(30))
            .build());

        final Court court = courtRepository.save(Court.builder()
            .name("Retention Test Court")
            .slug("retention-test-court-" + UUID.randomUUID())
            .open(true)
            .regionId(region.getId())
            .build());

        final CourtPhoto photo = courtPhotoRepository.save(CourtPhoto.builder()
            .courtId(court.getId())
            .fileLink("https://example.com/courts/test-photo.jpg")
            .updatedByUserId(inactiveUser.getId())
            .build());

        final int deletedUsers = userService.deleteInactiveUsers();

        assertThat(deletedUsers).isEqualTo(1);
        assertThat(userRepository.findById(inactiveUser.getId())).isEmpty();
        assertThat(courtPhotoRepository.findById(photo.getId()))
            .isPresent()
            .get()
            .extracting(CourtPhoto::getUpdatedByUserId)
            .isNull();
    }
}

