package uk.gov.hmcts.reform.fact.data.api.db;

import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCodes;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDxCode;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFacilities;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFax;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPostcode;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAreasOfLawRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCodesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDxCodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFacilitiesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFaxRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtPostcodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Feature("Court Change Updates")
@DisplayName("Court Change Updates")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
class CourtLastUpdatedTimeTest {

    private record TestCourts(Court court, Court controlCourt) {
    }

    @Autowired
    RegionRepository regionRepository;

    @Autowired
    CourtRepository courtRepository;

    @Autowired
    CourtCodesRepository courtCodesRepository;

    @Autowired
    CourtPostcodeRepository courtPostcodeRepository;

    @Autowired
    CourtFaxRepository courtFaxRepository;

    @Autowired
    CourtDxCodeRepository courtDxCodeRepository;

    @Autowired
    CourtFacilitiesRepository courtFacilitiesRepository;

    @Autowired
    CourtAreasOfLawRepository courtAreasOfLawRepository;

    TestCourts courts;
    ZonedDateTime controlCourtUpdate;

    @BeforeEach
    public void before() {
        courts = createTestAndControlCourts();
        controlCourtUpdate = courts.controlCourt.getLastUpdatedAt();
    }

    @AfterEach
    public void after() {
        courtAreasOfLawRepository.deleteAll();
        courtFacilitiesRepository.deleteAll();
        courtDxCodeRepository.deleteAll();
        courtFaxRepository.deleteAll();
        courtPostcodeRepository.deleteAll();
        courtCodesRepository.deleteAll();
        regionRepository.deleteAll();
        courtRepository.deleteAll();
    }

    @Test
    @DisplayName("Ensure court last_update_at column is updated on child addition")
    void addingChildTableUpdatesCourtLastUpdatedTimestamp() throws InterruptedException {

        // add some court codes and ensure that the court has been updated, and the control court has not.
        Thread.sleep(1);
        CourtCodes codes = CourtCodes.builder().courtId(courts.court.getId()).build();
        courtCodesRepository.save(codes);
        refreshCourtsAndCheckTimestamps();

        // as above for court postcodes
        Thread.sleep(1);
        CourtPostcode postcode = CourtPostcode.builder().courtId(courts.court.getId()).postcode("WR9 8JS").build();
        courtPostcodeRepository.save(postcode);
        refreshCourtsAndCheckTimestamps();
    }

    @Test
    @DisplayName("Ensure court last_update_at column is updated on child update")
    void updatingChildTableUpdatesCourtLastUpdatedTimestamp() throws InterruptedException {

        // create some data
        Thread.sleep(1);
        CourtFax fax = CourtFax.builder().courtId(courts.court.getId()).faxNumber("01234567891").build();
        fax = courtFaxRepository.save(fax);
        CourtDxCode dxCode = CourtDxCode.builder().courtId(courts.court.getId()).dxCode("9857983475").build();
        dxCode = courtDxCodeRepository.save(dxCode);
        refreshCourtsAndCheckTimestamps();

        // make a change to court fax
        Thread.sleep(1);
        fax.setFaxNumber("01324123423");
        courtFaxRepository.save(fax);
        refreshCourtsAndCheckTimestamps();

        // as above for court dx codes
        Thread.sleep(1);
        dxCode.setDxCode("576547234523");
        courtDxCodeRepository.save(dxCode);
        refreshCourtsAndCheckTimestamps();
    }


    @Test
    @DisplayName("Ensure court last_update_at column is updated on child removal")
    void removingChildTableUpdatesCourtLastUpdatedTimestamp() throws InterruptedException {

        // create some data
        Thread.sleep(1);
        CourtFacilities facitilites = CourtFacilities.builder()
            .courtId(courts.court.getId())
            .waitingArea(Boolean.TRUE)
            .waitingAreaChildren(Boolean.TRUE)
            .snackVendingMachines(Boolean.TRUE)
            .drinkVendingMachines(Boolean.TRUE)
            .wifi(Boolean.TRUE)
            .parking(Boolean.TRUE)
            .babyChanging(Boolean.TRUE)
            .quietRoom(Boolean.TRUE)
            .freeWaterDispensers(Boolean.TRUE)
            .cafeteria(Boolean.TRUE)
            .build();
        facitilites = courtFacilitiesRepository.save(facitilites);
        CourtAreasOfLaw areasOfLaw = CourtAreasOfLaw.builder().courtId(courts.court.getId()).build();
        areasOfLaw = courtAreasOfLawRepository.save(areasOfLaw);
        refreshCourtsAndCheckTimestamps();

        // remove court areas of law
        Thread.sleep(1);
        courtAreasOfLawRepository.delete(areasOfLaw);
        refreshCourtsAndCheckTimestamps();

        // remove court facilities
        Thread.sleep(1);
        courtFacilitiesRepository.delete(facitilites);
        refreshCourtsAndCheckTimestamps();

    }

    private ZonedDateTime refreshCourtsAndCheckTimestamps() {
        final ZonedDateTime courtUpdate = courts.court.getLastUpdatedAt();
        Court court = courtRepository.findById(courts.court().getId()).orElse(null);
        Court controlCourt = courtRepository.findById(courts.controlCourt().getId()).orElse(null);
        assertThat(court).isNotNull();
        assertThat(court.getLastUpdatedAt()).isAfter(courtUpdate);
        assertThat(controlCourt).isNotNull();
        assertThat(controlCourt.getLastUpdatedAt()).isEqualTo(controlCourtUpdate);
        courts = new TestCourts(court, controlCourt);
        return courts.court.getLastUpdatedAt();
    }

    private TestCourts createTestAndControlCourts() {
        // create an initial court object for monitoring
        Region region = Region.builder().name("test region").build();
        region = regionRepository.save(region);
        Court court = buildCourt(region.getId(), "Test Court", "test-court");
        courtRepository.save(court);
        assertThat(court).isNotNull();
        assertThat(court.getRegionId()).isEqualTo(region.getId());

        // create a control court for monitoring
        Court otherCourt = buildCourt(region.getId(), "Other Test Court", "other-test-court");
        courtRepository.save(otherCourt);
        return new TestCourts(court, otherCourt);
    }

    private Court buildCourt(UUID regionId, String name, String slug) {
        return Court.builder()
            .name(name)
            .slug(slug)
            .open(Boolean.TRUE)
            .warningNotice("Notice")
            .regionId(regionId)
            .isServiceCentre(Boolean.TRUE)
            .openOnCath(Boolean.TRUE)
            .mrdId("MRD123")
            .build();
    }
}
