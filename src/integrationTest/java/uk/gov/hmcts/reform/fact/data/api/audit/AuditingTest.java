package uk.gov.hmcts.reform.fact.data.api.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import uk.gov.hmcts.reform.fact.data.api.entities.Audit;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPhoto;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtTranslation;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AuditActionType;
import uk.gov.hmcts.reform.fact.data.api.repositories.AuditRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtPhotoRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtTranslationRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.services.AuditService;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.qameta.allure.Feature;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Feature("Background Auditing")
@DisplayName("Background Auditing")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class AuditingTest {

    private static final String TEST_COURT_NAME = "Test Court";
    // start of yesterday, just in case we get run at exactly the wrong nanosecond.
    private static final LocalDate CREATED_AFTER = LocalDate.now().minusDays(1);

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    CourtRepository courtRepository;

    @Autowired
    CourtTranslationRepository courtTranslationRepository;

    @Autowired
    CourtPhotoRepository courtPhotoRepository;

    @Autowired
    AuditRepository auditRepository;

    @Autowired
    AuditService auditService;

    @BeforeEach
    void setUp() {
        // and clear these down
        courtRepository.deleteAll();
        courtPhotoRepository.deleteAll();
        courtTranslationRepository.deleteAll();

        // do this one last
        auditRepository.deleteAll();
    }

    @Test
    @DisplayName("Creating and updating a Court should create Audit records")
    void creatingAndUpdatingACourtShouldCreateAuditRecords() {
        // INSERT
        Region region = regionRepository.findAll().getFirst();
        Court court = createCourt(region.getId(), TEST_COURT_NAME);
        courtRepository.save(court);

        // UPDATE
        court.setOpen(Boolean.TRUE);
        court.setName("Court Test Edit");
        courtRepository.save(court);

        assertCreateAndUpdateForEntity(court, Court.class.getSimpleName());
    }

    @Test
    @DisplayName("Creating and updating a CourtTranslation should create Audit records")
    void creatingAndUpdatingACourtTranslationShouldCreateAuditRecords() {

        // SETUP
        Region region = regionRepository.findAll().getFirst();
        Court court = createCourt(region.getId(), TEST_COURT_NAME);
        courtRepository.save(court);

        // INSERT
        CourtTranslation courtTranslation = CourtTranslation.builder()
            .courtId(court.getId())
            .phoneNumber("01234567890")
            .email("test@here.com")
            .build();
        courtTranslationRepository.save(courtTranslation);

        // UPDATE
        courtTranslation.setEmail("actually.me@there.com");
        courtTranslationRepository.save(courtTranslation);

        assertCreateAndUpdateForEntity(court, CourtTranslation.class.getSimpleName());
    }

    @Test
    @DisplayName("Creating, updating and deleting a CourtPhoto should create Audit records")
    void creatingUpdatingAndDeletingACourtPhotoShouldCreateAuditRecords() {

        // SETUP
        Region region = regionRepository.findAll().getFirst();
        Court court = createCourt(region.getId(), TEST_COURT_NAME);
        courtRepository.save(court);

        // INSERT
        CourtPhoto courtPhoto = CourtPhoto.builder()
            .courtId(court.getId())
            .fileLink("https://example.com/image.png")
            .build();
        courtPhotoRepository.save(courtPhoto);

        // UPDATE
        courtPhoto.setFileLink("https://example.com/update.png");
        courtPhotoRepository.save(courtPhoto);

        // DELETE
        courtPhotoRepository.delete(courtPhoto);

        assertCreateUpdateAndDeleteForEntity(court, CourtPhoto.class.getSimpleName());
    }

    @Test
    @DisplayName("Creating entities on multiple threads should correctly audit")
    void creatingEntitiesOnMultipleThreadsShouldCreateAuditRecords() throws InterruptedException {

        // in total this will perform 300 updates across three threads
        final int updateCount = 100;

        // SETUP
        Region region = regionRepository.findAll().getFirst();
        final Court court = createCourt(region.getId(), TEST_COURT_NAME);
        courtRepository.save(court);

        final CourtPhoto courtPhoto = CourtPhoto.builder()
            .courtId(court.getId())
            .fileLink("https://example.com/image.png")
            .build();
        courtPhotoRepository.save(courtPhoto);

        final CourtTranslation courtTranslation = CourtTranslation.builder()
            .courtId(court.getId())
            .phoneNumber("01234567890")
            .email("test@here.com")
            .build();
        courtTranslationRepository.save(courtTranslation);

        final CountDownLatch latch = new CountDownLatch(1);

        // make changes
        Thread threadA = new Thread(() -> {
            synchronized (latch) {
                try {
                    latch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            // lot of updates
            for (int i = 0; i < updateCount; i++) {
                court.setName("Court Test " + RandomStringUtils.insecure().nextAlphabetic(6));
                courtRepository.save(court);
            }
        });

        Thread threadB = new Thread(() -> {
            synchronized (latch) {
                try {
                    latch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            // lot of updates
            for (int i = 0; i < updateCount; i++) {
                courtTranslation.setEmail(RandomStringUtils.insecure().nextAlphabetic(6) + "@here.com");
                courtTranslationRepository.save(courtTranslation);
            }
        });

        Thread threadC = new Thread(() -> {
            synchronized (latch) {
                try {
                    latch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            // lot of updates
            for (int i = 0; i < updateCount; i++) {
                courtPhoto.setFileLink(String.format(
                    "https://example.com/%s.png",
                    RandomStringUtils.insecure().nextAlphabetic(6)
                ));
                courtPhotoRepository.save(courtPhoto);
            }
        });

        // all threads should park waiting for the latch
        threadA.start();
        threadB.start();
        threadC.start();

        // start them
        latch.countDown();

        // join them
        threadA.join(Duration.ofSeconds(10));
        threadB.join(Duration.ofSeconds(10));
        threadC.join(Duration.ofSeconds(10));

        // It's great that we got here, but we need to check that we have all
        // the audit records we're expecting
        List<AuditActionType> expectedActions = new ArrayList<>();
        // a load of updates
        for (int i = 0; i < updateCount; i++) {
            expectedActions.add(AuditActionType.UPDATE);
        }
        // and one insert
        expectedActions.add(AuditActionType.INSERT);
        AuditActionType[] expectedAuditActionTypes = expectedActions.toArray(new AuditActionType[0]);

        assertAuditActionsForEntity(
            court,
            Court.class.getSimpleName(),
            expectedAuditActionTypes
        );

        assertAuditActionsForEntity(
            court,
            CourtPhoto.class.getSimpleName(),
            expectedAuditActionTypes
        );

        assertAuditActionsForEntity(
            court,
            CourtTranslation.class.getSimpleName(),
            expectedAuditActionTypes
        );

    }

    // bottled assertions

    private void assertCreateUpdateAndDeleteForEntity(Court owningCourt, String testedEntity) {
        assertAuditActionsForEntity(
            owningCourt,
            testedEntity,
            AuditActionType.DELETE,
            AuditActionType.UPDATE,
            AuditActionType.INSERT
        );
    }

    private void assertCreateAndUpdateForEntity(Court owningCourt, String testedEntity) {
        assertAuditActionsForEntity(
            owningCourt,
            testedEntity,
            AuditActionType.UPDATE,
            AuditActionType.INSERT
        );
    }

    @SuppressWarnings({"java:S5960"})
    private void assertAuditActionsForEntity(Court owningCourt, String testedEntity, AuditActionType... actionTypes) {
        // list audits filtered by court id
        Page<Audit> audits = auditService.getFilteredAndPaginatedAudits(
            0,
            1000,
            CREATED_AFTER,
            null,
            owningCourt.getId().toString(),
            null
        );

        // asserts (remember that audits are returned in descending date order)
        assertNotNull(audits);
        assertNotNull(audits.getContent());

        // NOTE: We're just testing that the audits we wanted to see as side effects of
        // actions on an entity are present. Testing for paging, filtering, etc. is
        // handled in other tests

        // pull the audits for the entity we're interested in
        List<Audit> content = audits.getContent().stream().filter(c -> Objects.equals(
            c.getActionEntity(),
            testedEntity
        )).toList();

        // should have the right number of actions
        assertEquals(actionTypes.length, content.size());

        // in the right order
        for (int i = 0; i < actionTypes.length; i++) {
            assertEquals(actionTypes[i], content.get(i).getActionType());
        }
    }

    // entity creation methods

    private Court createCourt(UUID regionId, String name) {
        return Court.builder()
            .name(name)
            .regionId(regionId)
            .isServiceCentre(Boolean.FALSE)
            .build();
    }
}
