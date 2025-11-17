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

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import io.qameta.allure.Feature;
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
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.config.name=application-test"
)
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
        courtPhoto.setFileLink("https://example.com/image.png");
        courtPhotoRepository.save(courtPhoto);

        // DELETE
        courtPhotoRepository.delete(courtPhoto);

        assertCreateUpdateAndDeleteForEntity(court, CourtPhoto.class.getSimpleName());
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
            100,
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
