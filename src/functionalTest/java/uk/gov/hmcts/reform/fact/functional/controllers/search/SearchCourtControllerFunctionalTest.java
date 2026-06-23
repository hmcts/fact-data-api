package uk.gov.hmcts.reform.fact.functional.controllers.search;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistanceResponse;
import uk.gov.hmcts.reform.fact.data.api.dto.SearchResult;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AddressType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchResultType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.ServiceAreaType;
import uk.gov.hmcts.reform.fact.data.api.models.AreaOfLawSelectionDto;
import uk.gov.hmcts.reform.fact.data.api.models.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.models.LocalAuthoritySelectionDto;
import uk.gov.hmcts.reform.fact.data.api.os.OsDpa;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Feature("Search Court Controller")
@DisplayName("Search Court Controller")
public final class SearchCourtControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = JsonMapper.builder()
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();
    private static final String regionId = TestDataHelper.fetchFirstRegionId(http);
    private static final String STABLE_ENGLAND_POSTCODE = "SW1A 0AA";
    private static final String TEST_COURT_PREFIX = "Test Court";
    private static final String TEST_SERVICE_CENTRE_PREFIX = "Test Service Centre Search";

    @Test
    @DisplayName("GET /search/courts/v1/name returns courts matching query")
    void shouldReturnCourtsMatchingNameQuery() throws Exception {
        final String courtName = TEST_COURT_PREFIX + " Birmingham";
        createOpenCourt(courtName);

        final Response searchResponse = http.doGet("/search/courts/v1/name?q=Birmingham");

        assertThat(searchResponse.statusCode())
            .as("Expected 200 OK for court name search")
            .isEqualTo(OK.value());

        final List<Court> courts = mapper.readValue(
            searchResponse.getBody().asString(),
            new TypeReference<List<Court>>() {}
        );

        assertThat(courts)
            .as("Expected to find the created court '%s' in search results", courtName)
            .extracting(Court::getName)
            .anyMatch(name -> name.startsWith(courtName));
    }

    @Test
    @DisplayName("GET /search/courts/v1/prefix returns courts starting with letter")
    void shouldReturnCourtsStartingWithPrefix() throws Exception {
        final String courtName = TEST_COURT_PREFIX + " Alpha";
        createOpenCourt(courtName);

        final Response searchResponse = http.doGet("/search/courts/v1/prefix?prefix=T");

        assertThat(searchResponse.statusCode())
            .as("Expected 200 OK for prefix search")
            .isEqualTo(OK.value());

        final List<Court> courts = mapper.readValue(
            searchResponse.getBody().asString(),
            new TypeReference<List<Court>>() {}
        );

        assertThat(courts)
            .as("Expected to find courts starting with 'T'")
            .isNotEmpty()
            .extracting(Court::getName)
            .as("All returned courts should start with 'T'")
            .allMatch(name -> name.toUpperCase().startsWith("T"))
            .as("Expected to find the created court '%s' in prefix results", courtName)
            .anyMatch(name -> name.startsWith(courtName));
    }

    @Test
    @DisplayName("GET /search/courts/v1/postcode returns nearest courts for postcode-only search")
    void shouldReturnNearestCourtsForPostcodeOnlySearch() throws Exception {
        final OsDpa dpa = TestDataHelper.fetchFirstDpaForPostcode(http, STABLE_ENGLAND_POSTCODE);

        final String courtName = TEST_COURT_PREFIX + " Postcode Search";
        final UUID courtId = createOpenCourt(courtName);

        final CourtAddress address = buildVisitUsAddressFromDpa(courtId, dpa).build();

        final Response addressResponse = http.doPost("/courts/" + courtId + "/v1/address", address);

        assertThat(addressResponse.statusCode())
            .as("Expected 201 CREATED when creating address for court %s", courtId)
            .isEqualTo(CREATED.value());

        final Response searchResponse = http.doGet("/search/courts/v1/postcode?postcode="
                                                       + STABLE_ENGLAND_POSTCODE);

        assertThat(searchResponse.statusCode())
            .as("Expected 200 OK for postcode-only search")
            .isEqualTo(OK.value());

        final List<CourtWithDistanceResponse> courts = mapper.readValue(
            searchResponse.getBody().asString(),
            new TypeReference<List<CourtWithDistanceResponse>>() {}
        );

        assertThat(courts)
            .as("Expected non-empty results for postcode search")
            .isNotEmpty()
            .as("Each result should have court ID, name, and distance")
            .allSatisfy(court -> {
                assertThat(court.getCourtId())
                    .as("Court ID should be present")
                    .isNotNull();
                assertThat(court.getCourtName())
                    .as("Court name should be present")
                    .isNotBlank();
                assertThat(court.getDistance())
                    .as("Distance should be present and non-negative")
                    .isNotNull()
                    .isGreaterThanOrEqualTo(BigDecimal.ZERO);
            });

        assertThat(courts)
            .as("Expected to find the created court '%s' in search results", courtName)
            .extracting(CourtWithDistanceResponse::getCourtId)
            .contains(courtId);
    }

    /**
     * Strategy coverage for {@code GET /search/courts/v1/postcode} when {@code serviceArea} and {@code action}
     * are supplied.
     *
     * <p>
     * These tests cover:
     * - CIVIL_POSTCODE_PREFERENCE (civil service areas)
     * - DEFAULT_AOL_DISTANCE (other service areas)
     * - FAMILY_NON_REGIONAL (family service areas)
     * - Childcare SPOE special-case (single point of entry search)
     */
    @Test
    @DisplayName("GET /search/courts/v1/postcode returns courts for OTHER type service area using DEFAULT_AOL_DISTANCE")
    void shouldReturnCourtsForOtherTypeServiceAreaWithDefaultAolDistanceStrategy() throws Exception {
        final String serviceAreaName = "Tax";

        final Response serviceAreasResponse = http.doGet("/types/v1/service-areas");

        assertThat(serviceAreasResponse.statusCode())
            .as("Expected 200 OK when fetching service areas")
            .isEqualTo(OK.value());

        final List<ServiceArea> serviceAreas = mapper.readValue(
            serviceAreasResponse.getBody().asString(),
            new TypeReference<List<ServiceArea>>() {}
        );

        final List<ServiceArea> matchingServiceAreas = serviceAreas.stream()
            .filter(serviceArea -> serviceAreaName.equals(serviceArea.getName()))
            .toList();

        assertThat(matchingServiceAreas)
            .as("Expected to find exactly one service area named '%s'", serviceAreaName)
            .hasSize(1);

        final ServiceArea taxServiceArea = matchingServiceAreas.getFirst();

        assertThat(taxServiceArea.getType())
            .as("Expected service area '%s' to have type OTHER", serviceAreaName)
            .isEqualTo(ServiceAreaType.OTHER);

        final UUID taxAreaOfLawId = taxServiceArea.getAreaOfLawId();

        assertThat(taxAreaOfLawId)
            .as("Service area '%s' should have an area of law ID", serviceAreaName)
            .isNotNull();

        final OsDpa dpa = TestDataHelper.fetchFirstDpaForPostcode(http, STABLE_ENGLAND_POSTCODE);

        final String courtName = TEST_COURT_PREFIX + " Tax Service Area Search";
        final UUID courtId = createOpenCourt(courtName);

        final CourtAddress address = buildVisitUsAddressFromDpa(courtId, dpa).build();

        final Response addressResponse = http.doPost("/courts/" + courtId + "/v1/address", address);

        assertThat(addressResponse.statusCode())
            .as("Expected 201 CREATED when creating address for court %s", courtId)
            .isEqualTo(CREATED.value());

        final CourtAreasOfLaw courtAreasOfLaw = TestDataHelper.buildCourtAreasOfLaw(
            courtId,
            List.of(taxAreaOfLawId)
        );

        final Response areasOfLawResponse = http.doPut(
            "/courts/" + courtId + "/v1/areas-of-law",
            courtAreasOfLaw
        );

        assertThat(areasOfLawResponse.statusCode())
            .as("Expected 201 CREATED when setting areas of law for court %s", courtId)
            .isEqualTo(CREATED.value());

        final Response searchResponse = http.doGet(
            "/search/courts/v1/postcode",
            Map.of(
                "postcode", STABLE_ENGLAND_POSTCODE,
                "serviceArea", serviceAreaName,
                "action", "DOCUMENTS"
            )
        );

        assertThat(searchResponse.statusCode())
            .as("Expected 200 OK for postcode search with OTHER type service area")
            .isEqualTo(OK.value());

        final List<CourtWithDistanceResponse> courts = mapper.readValue(
            searchResponse.getBody().asString(),
            new TypeReference<List<CourtWithDistanceResponse>>() {}
        );

        assertThat(courts)
            .as("Expected non-empty results for OTHER type service area search")
            .isNotEmpty()
            .as("Expected to find the created court '%s' in search results", courtName)
            .extracting(CourtWithDistanceResponse::getCourtId)
            .contains(courtId);
    }

    @Test
    @DisplayName("GET /search/courts/v1/postcode "
        + "returns courts for CIVIL type service area using CIVIL_POSTCODE_PREFERENCE")
    void shouldReturnCourtsForCivilServiceAreaUsingCivilPostcodePreferenceStrategy() throws Exception {
        final Response serviceAreasResponse = http.doGet("/types/v1/service-areas");

        assertThat(serviceAreasResponse.statusCode())
            .as("Expected 200 OK when fetching service areas")
            .isEqualTo(OK.value());

        final List<ServiceArea> serviceAreas = mapper.readValue(
            serviceAreasResponse.getBody().asString(),
            new TypeReference<List<ServiceArea>>() {}
        );

        final List<ServiceArea> civilServiceAreas = serviceAreas.stream()
            .filter(serviceArea -> ServiceAreaType.CIVIL.equals(serviceArea.getType()))
            .toList();

        assertThat(civilServiceAreas)
            .as("Expected at least one CIVIL service area to exist")
            .isNotEmpty();

        final ServiceArea civilServiceArea = civilServiceAreas.getFirst();

        assertThat(civilServiceArea.getName())
            .as("Expected CIVIL service area to have a name")
            .isNotBlank();

        assertThat(civilServiceArea.getAreaOfLawId())
            .as("Expected CIVIL service area '%s' to have an area of law ID", civilServiceArea.getName())
            .isNotNull();

        final OsDpa dpa = TestDataHelper.fetchFirstDpaForPostcode(http, STABLE_ENGLAND_POSTCODE);

        final String courtName = TEST_COURT_PREFIX + " Civil Service Area Search";
        final UUID courtId = createOpenCourt(courtName);

        final CourtAddress address = buildVisitUsAddressFromDpa(courtId, dpa).build();

        final Response addressResponse = http.doPost("/courts/" + courtId + "/v1/address", address);

        assertThat(addressResponse.statusCode())
            .as("Expected 201 CREATED when creating address for court %s", courtId)
            .isEqualTo(CREATED.value());

        final CourtAreasOfLaw courtAreasOfLaw = TestDataHelper.buildCourtAreasOfLaw(
            courtId,
            List.of(civilServiceArea.getAreaOfLawId())
        );

        final Response areasOfLawResponse = http.doPut(
            "/courts/" + courtId + "/v1/areas-of-law",
            courtAreasOfLaw
        );

        assertThat(areasOfLawResponse.statusCode())
            .as("Expected 201 CREATED when setting areas of law for court %s", courtId)
            .isEqualTo(CREATED.value());

        final Response searchResponse = http.doGet(
            "/search/courts/v1/postcode",
            Map.of(
                "postcode", STABLE_ENGLAND_POSTCODE,
                "serviceArea", civilServiceArea.getName(),
                "action", "DOCUMENTS"
            )
        );

        assertThat(searchResponse.statusCode())
            .as("Expected 200 OK for CIVIL service area postcode search (serviceArea=%s)",
                civilServiceArea.getName())
            .isEqualTo(OK.value());

        final List<CourtWithDistanceResponse> courts = mapper.readValue(
            searchResponse.getBody().asString(),
            new TypeReference<List<CourtWithDistanceResponse>>() {}
        );

        assertThat(courts)
            .as("Expected non-empty results for CIVIL service area search")
            .isNotEmpty()
            .as("Expected to find the created court '%s' in search results", courtName)
            .extracting(CourtWithDistanceResponse::getCourtId)
            .contains(courtId);
    }

    @Test
    @DisplayName("GET /search/courts/v1/postcode returns courts for FAMILY type service area using FAMILY_NON_REGIONAL")
    void shouldReturnCourtsForFamilyServiceAreaUsingFamilyNonRegionalStrategy() throws Exception {
        final String familyServiceAreaName = "Adoption";

        final Response serviceAreasResponse = http.doGet("/types/v1/service-areas");

        assertThat(serviceAreasResponse.statusCode())
            .as("Expected 200 OK when fetching service areas")
            .isEqualTo(OK.value());

        final List<ServiceArea> serviceAreas = mapper.readValue(
            serviceAreasResponse.getBody().asString(),
            new TypeReference<List<ServiceArea>>() {}
        );

        final List<ServiceArea> matchingServiceAreas = serviceAreas.stream()
            .filter(serviceArea -> familyServiceAreaName.equals(serviceArea.getName()))
            .toList();

        assertThat(matchingServiceAreas)
            .as("Expected to find exactly one service area named '%s'", familyServiceAreaName)
            .hasSize(1);

        final ServiceArea familyServiceArea = matchingServiceAreas.getFirst();

        assertThat(familyServiceArea.getType())
            .as("Expected service area '%s' to have type FAMILY", familyServiceAreaName)
            .isEqualTo(ServiceAreaType.FAMILY);

        final UUID familyAreaOfLawId = familyServiceArea.getAreaOfLawId();

        assertThat(familyAreaOfLawId)
            .as("Expected FAMILY service area '%s' to have an area of law ID", familyServiceAreaName)
            .isNotNull();

        final OsDpa dpa = TestDataHelper.fetchFirstDpaForPostcode(http, STABLE_ENGLAND_POSTCODE);

        final String expectedLocalAuthorityName = "City of Westminster";
        final Integer localCustodianCode = dpa.getLocalCustodianCode();
        final String localCustodianCodeDescription = dpa.getLocalCustodianCodeDescription();

        assertThat(localCustodianCode)
            .as("Expected OS DPA to contain local custodian code for postcode %s", STABLE_ENGLAND_POSTCODE)
            .isNotNull();

        assertThat(localCustodianCodeDescription)
            .as("Expected OS DPA to contain local custodian code description for postcode %s",
                STABLE_ENGLAND_POSTCODE)
            .isNotBlank()
            .as(
                "Expected stable postcode %s to resolve to local authority '%s' but was '%s' (code=%s)",
                STABLE_ENGLAND_POSTCODE,
                expectedLocalAuthorityName,
                localCustodianCodeDescription,
                localCustodianCode
            )
            .containsIgnoringCase("Westminster");

        final String unmappedCourtName = TEST_COURT_PREFIX + " Family Search Unmapped";
        final UUID unmappedCourtId = createOpenCourt(unmappedCourtName);

        final Response unmappedAddressResponse = http.doPost(
            "/courts/" + unmappedCourtId + "/v1/address",
            buildVisitUsAddressFromDpa(unmappedCourtId, dpa).build()
        );

        assertThat(unmappedAddressResponse.statusCode())
            .as("Expected 201 CREATED when creating address for court %s", unmappedCourtId)
            .isEqualTo(CREATED.value());

        final CourtAreasOfLaw unmappedCourtAreasOfLaw = TestDataHelper.buildCourtAreasOfLaw(
            unmappedCourtId,
            List.of(familyAreaOfLawId)
        );

        final Response unmappedAreasOfLawResponse = http.doPut(
            "/courts/" + unmappedCourtId + "/v1/areas-of-law",
            unmappedCourtAreasOfLaw
        );

        assertThat(unmappedAreasOfLawResponse.statusCode())
            .as("Expected 201 CREATED when setting areas of law for court %s", unmappedCourtId)
            .isEqualTo(CREATED.value());

        final String mappedCourtName = TEST_COURT_PREFIX + " Family Search Mapped";
        final UUID mappedCourtId = createOpenCourt(mappedCourtName);

        final Response mappedAddressResponse = http.doPost(
            "/courts/" + mappedCourtId + "/v1/address",
            buildVisitUsAddressFromDpa(mappedCourtId, dpa).build()
        );

        assertThat(mappedAddressResponse.statusCode())
            .as("Expected 201 CREATED when creating address for court %s", mappedCourtId)
            .isEqualTo(CREATED.value());

        final CourtAreasOfLaw mappedCourtAreasOfLaw = TestDataHelper.buildCourtAreasOfLaw(
            mappedCourtId,
            List.of(familyAreaOfLawId)
        );

        final Response mappedAreasOfLawResponse = http.doPut(
            "/courts/" + mappedCourtId + "/v1/areas-of-law",
            mappedCourtAreasOfLaw
        );

        assertThat(mappedAreasOfLawResponse.statusCode())
            .as("Expected 201 CREATED when setting areas of law for court %s", mappedCourtId)
            .isEqualTo(CREATED.value());

        final Response localAuthoritiesResponse = http.doGet("/courts/" + mappedCourtId
                                                                 + "/v1/local-authorities");

        assertThat(localAuthoritiesResponse.statusCode())
            .as("Expected 200 OK when fetching local authorities for court %s", mappedCourtId)
            .isEqualTo(OK.value());

        final List<CourtLocalAuthorityDto> localAuthoritiesByAreaOfLaw = mapper.readValue(
            localAuthoritiesResponse.getBody().asString(),
            new TypeReference<List<CourtLocalAuthorityDto>>() {}
        );

        final List<CourtLocalAuthorityDto> matchingAreaOfLawAuthorities = localAuthoritiesByAreaOfLaw.stream()
            .filter(dto -> familyAreaOfLawId.equals(dto.getAreaOfLawId()))
            .toList();

        assertThat(matchingAreaOfLawAuthorities)
            .as("Expected local authorities mapping to exist for area of law ID %s", familyAreaOfLawId)
            .hasSize(1);

        final CourtLocalAuthorityDto familyLocalAuthorities = matchingAreaOfLawAuthorities.getFirst();

        final List<LocalAuthoritySelectionDto> matchingLocalAuthorities = familyLocalAuthorities.getLocalAuthorities()
            .stream()
            .filter(la -> expectedLocalAuthorityName.equalsIgnoreCase(la.getName()))
            .toList();

        assertThat(matchingLocalAuthorities)
            .as("Expected to find local authority '%s' for court local authority configuration",
                expectedLocalAuthorityName)
            .hasSize(1);

        final LocalAuthoritySelectionDto targetLocalAuthority = matchingLocalAuthorities.getFirst();

        final CourtLocalAuthorityDto update = CourtLocalAuthorityDto.builder()
            .areaOfLawId(familyAreaOfLawId)
            .localAuthorities(List.of(
                LocalAuthoritySelectionDto.builder()
                    .id(targetLocalAuthority.getId())
                    .selected(true)
                    .build()
            ))
            .build();

        final Response updateLocalAuthoritiesResponse = http.doPut(
            "/courts/" + mappedCourtId + "/v1/local-authorities",
            List.of(update)
        );

        assertThat(updateLocalAuthoritiesResponse.statusCode())
            .as("Expected 200 OK when updating local authorities for court %s", mappedCourtId)
            .isEqualTo(OK.value());

        final Response searchResponse = http.doGet(
            "/search/courts/v1/postcode",
            Map.of(
                "postcode", STABLE_ENGLAND_POSTCODE,
                "serviceArea", familyServiceAreaName,
                "action", "DOCUMENTS"
            )
        );

        assertThat(searchResponse.statusCode())
            .as("Expected 200 OK for FAMILY service area postcode search (serviceArea=%s)",
                familyServiceAreaName)
            .isEqualTo(OK.value());

        final List<CourtWithDistanceResponse> courts = mapper.readValue(
            searchResponse.getBody().asString(),
            new TypeReference<List<CourtWithDistanceResponse>>() {}
        );

        assertThat(courts)
            .as("Expected non-empty results for FAMILY service area search")
            .isNotEmpty()
            .as("Expected to find the mapped court '%s' in search results", mappedCourtName)
            .extracting(CourtWithDistanceResponse::getCourtId)
            .contains(mappedCourtId)
            .as("Expected unmapped court '%s' not to be returned for FAMILY non-regional search",
                unmappedCourtName)
            .doesNotContain(unmappedCourtId);
    }

    @Test
    @DisplayName("GET /search/locations/v1/postcode returns regional service centre for FAMILY service area")
    void shouldReturnRegionalServiceCentreForFamilyServiceAreaLocationSearch() throws Exception {
        final String familyServiceAreaName = "Forced marriage";

        final Response serviceAreasResponse = http.doGet("/types/v1/service-areas");

        assertThat(serviceAreasResponse.statusCode())
            .as("Expected 200 OK when fetching service areas")
            .isEqualTo(OK.value());

        final List<ServiceArea> serviceAreas = mapper.readValue(
            serviceAreasResponse.getBody().asString(),
            new TypeReference<List<ServiceArea>>() {}
        );

        final List<ServiceArea> matchingServiceAreas = serviceAreas.stream()
            .filter(serviceArea -> familyServiceAreaName.equals(serviceArea.getName()))
            .toList();

        assertThat(matchingServiceAreas)
            .as("Expected to find exactly one service area named '%s'", familyServiceAreaName)
            .hasSize(1);

        final ServiceArea familyServiceArea = matchingServiceAreas.getFirst();

        assertThat(familyServiceArea.getType())
            .as("Expected service area '%s' to have type FAMILY", familyServiceAreaName)
            .isEqualTo(ServiceAreaType.FAMILY);

        assertThat(familyServiceArea.getAreaOfLawId())
            .as("Expected FAMILY service area '%s' to have an area of law ID", familyServiceAreaName)
            .isNotNull();

        final OsDpa dpa = TestDataHelper.fetchFirstDpaForPostcode(http, STABLE_ENGLAND_POSTCODE);
        final String serviceCentreName = TEST_SERVICE_CENTRE_PREFIX + " Family Regional";
        final UUID serviceCentreId = createOpenRegionalServiceCentre(
            serviceCentreName,
            familyServiceArea,
            dpa
        );

        final Response searchResponse = http.doGet(
            "/search/locations/v1/postcode",
            Map.of(
                "postcode", STABLE_ENGLAND_POSTCODE,
                "serviceArea", familyServiceAreaName,
                "action", "DOCUMENTS"
            )
        );

        assertThat(searchResponse.statusCode())
            .as("Expected 200 OK for FAMILY regional service-centre postcode search (serviceArea=%s)",
                familyServiceAreaName)
            .isEqualTo(OK.value());

        final List<SearchResult> results = mapper.readValue(
            searchResponse.getBody().asString(),
            new TypeReference<List<SearchResult>>() {}
        );

        assertThat(results)
            .as("Expected regional service centre '%s' in location search results", serviceCentreName)
            .anySatisfy(result -> {
                assertThat(result.getId()).isEqualTo(serviceCentreId);
                assertThat(result.getName()).startsWith(serviceCentreName);
                assertThat(result.getType()).isEqualTo(SearchResultType.SERVICE_CENTRE);
                assertThat(result.getDistance()).isNotNull();
            });
    }

    @Test
    @DisplayName("GET /search/courts/v1/postcode returns nearest SPOE court for childcare arrangements")
    void shouldReturnNearestSpoeCourtForChildcareArrangementsServiceArea() throws Exception {
        final String childcareServiceAreaName = "Childcare arrangements if you separate from your partner";

        final OsDpa dpa = TestDataHelper.fetchFirstDpaForPostcode(http, STABLE_ENGLAND_POSTCODE);

        final String courtName = TEST_COURT_PREFIX + " Childcare SPOE Search";
        final UUID courtId = createOpenCourt(courtName);

        final Response addressResponse = http.doPost(
            "/courts/" + courtId + "/v1/address",
            buildVisitUsAddressFromDpa(courtId, dpa).build()
        );

        assertThat(addressResponse.statusCode())
            .as("Expected 201 CREATED when creating address for SPOE court %s", courtId)
            .isEqualTo(CREATED.value());

        final UUID childrenAreaOfLawId = TestDataHelper.getAreaOfLawIdByName(http, "Children");

        final Response updateSinglePointOfEntryResponse = http.doPut(
            "/courts/" + courtId + "/v1/single-point-of-entry",
            List.of(AreaOfLawSelectionDto.builder().id(childrenAreaOfLawId).selected(true).build())
        );

        assertThat(updateSinglePointOfEntryResponse.statusCode())
            .as("Expected 200 OK when updating single point of entry for court %s", courtId)
            .isEqualTo(OK.value());

        final Response searchResponse = http.doGet(
            "/search/courts/v1/postcode",
            Map.of(
                "postcode", STABLE_ENGLAND_POSTCODE,
                "serviceArea", childcareServiceAreaName,
                "action", "DOCUMENTS"
            )
        );

        assertThat(searchResponse.statusCode())
            .as("Expected 200 OK for childcare SPOE postcode search (serviceArea=%s)",
                childcareServiceAreaName)
            .isEqualTo(OK.value());

        final List<CourtWithDistanceResponse> courts = mapper.readValue(
            searchResponse.getBody().asString(),
            new TypeReference<List<CourtWithDistanceResponse>>() {}
        );

        assertThat(courts)
            .as("Expected exactly one SPOE court to be returned for childcare arrangements")
            .hasSize(1);

        assertThat(courts.getFirst().getCourtId())
            .as("Expected SPOE search to return the created court '%s'", courtName)
            .isEqualTo(courtId);
    }

    @Test
    @DisplayName("GET /search/courts/v1/postcode returns 400 when serviceArea provided without action")
    void shouldReturn400WhenServiceAreaProvidedWithoutAction() {
        final Response response = http.doGet("/search/courts/v1/postcode", Map.of(
            "postcode", STABLE_ENGLAND_POSTCODE,
            "serviceArea", "Money claims"
        ));

        assertThat(response.statusCode())
            .as("Expected 400 Bad Request when serviceArea provided without action")
            .isEqualTo(BAD_REQUEST.value());
    }

    @Test
    @DisplayName("GET /search/courts/v1/postcode returns 404 for non-existent service area")
    void shouldReturn404ForNonExistentServiceAreaInPostcodeSearch() {
        final Response response = http.doGet("/search/courts/v1/postcode", Map.of(
            "postcode", STABLE_ENGLAND_POSTCODE,
            "serviceArea", "Non Existent Service Area",
            "action", "NEAREST"
        ));

        assertThat(response.statusCode())
            .as("Expected 404 Not Found for non-existent service area")
            .isEqualTo(NOT_FOUND.value());
    }

    @Test
    @DisplayName("GET /search/courts/v1/postcode respects limit parameter")
    void shouldRespectLimitParameterInPostcodeSearch() throws Exception {
        final Response searchResponse = http.doGet("/search/courts/v1/postcode", Map.of(
            "postcode", STABLE_ENGLAND_POSTCODE,
            "limit", 2
        ));

        assertThat(searchResponse.statusCode())
            .as("Expected 200 OK for postcode search with limit")
            .isEqualTo(OK.value());

        final List<CourtWithDistanceResponse> courts = mapper.readValue(
            searchResponse.getBody().asString(),
            new TypeReference<List<CourtWithDistanceResponse>>() {}
        );

        assertThat(courts)
            .as("Expected at most 2 results when limit=2")
            .hasSizeLessThanOrEqualTo(2);
    }

    /**
     * Creates a court and updates it to open so it can be returned in search results.
     *
     * @param courtName the court name
     * @return the created court ID
     */
    private static UUID createOpenCourt(final String courtName) {
        final UUID courtId = TestDataHelper.createCourt(http, courtName);

        final Court courtToUpdate = new Court();
        courtToUpdate.setName(TestDataHelper.appendRandomSuffixToCourtName(courtName));
        courtToUpdate.setRegionId(UUID.fromString(regionId));
        courtToUpdate.setOpen(true);

        final Response updateResponse = http.doPut("/courts/" + courtId + "/v1", courtToUpdate);

        assertThat(updateResponse.statusCode())
            .as("Expected 200 OK when updating court %s to open", courtId)
            .isEqualTo(OK.value());

        return courtId;
    }

    /**
     * Builds a visit address payload for a court based on OS Delivery Point Address (DPA) data.
     *
     * @param courtId the court ID
     * @param dpa the OS DPA data
     * @return the court address payload
     */
    private static CourtAddress.CourtAddressBuilder buildVisitUsAddressFromDpa(final UUID courtId, final OsDpa dpa) {
        return CourtAddress.builder()
            .courtId(courtId)
            .addressLine1(dpa.getAddress())
            .townCity(dpa.getPostTown())
            .postcode(dpa.getPostcode())
            .addressType(AddressType.VISIT_US)
            .lat(BigDecimal.valueOf(dpa.getLat()))
            .lon(BigDecimal.valueOf(dpa.getLng()));
    }

    private static UUID createOpenRegionalServiceCentre(
        final String serviceCentreName,
        final ServiceArea serviceArea,
        final OsDpa dpa
    ) {
        final ServiceCentre serviceCentre = new ServiceCentre();
        serviceCentre.setName(TestDataHelper.appendRandomSuffixToCourtName(serviceCentreName));
        serviceCentre.setOpen(true);
        serviceCentre.setServiceAreaIds(List.of(serviceArea.getId()));
        serviceCentre.setCatchmentType(CatchmentType.REGIONAL);

        final Response createResponse = http.doPost("/service-centres/v1", serviceCentre);

        assertThat(createResponse.statusCode())
            .as("Expected 201 CREATED when creating regional service centre")
            .isEqualTo(CREATED.value());

        final UUID serviceCentreId = UUID.fromString(createResponse.jsonPath().getString("id"));
        serviceCentre.setOpen(true);

        final Response updateResponse = http.doPut("/service-centres/" + serviceCentreId + "/v1", serviceCentre);

        assertThat(updateResponse.statusCode())
            .as("Expected 200 OK when updating regional service centre %s to open", serviceCentreId)
            .isEqualTo(OK.value());

        final ServiceCentreAddress address = ServiceCentreAddress.builder()
            .serviceCentreId(serviceCentreId)
            .addressLine1(dpa.getAddress())
            .townCity(dpa.getPostTown())
            .postcode(dpa.getPostcode())
            .addressType(AddressType.VISIT_US)
            .lat(BigDecimal.valueOf(dpa.getLat()))
            .lon(BigDecimal.valueOf(dpa.getLng()))
            .build();

        final Response addressResponse = http.doPost(
            "/service-centres/" + serviceCentreId + "/v1/address",
            address
        );

        assertThat(addressResponse.statusCode())
            .as("Expected 201 CREATED when creating address for service centre %s", serviceCentreId)
            .isEqualTo(CREATED.value());

        final ServiceCentreAreasOfLaw areasOfLaw = ServiceCentreAreasOfLaw.builder()
            .serviceCentreId(serviceCentreId)
            .areasOfLaw(List.of(serviceArea.getAreaOfLawId()))
            .build();

        final Response areasOfLawResponse = http.doPut(
            "/service-centres/" + serviceCentreId + "/v1/areas-of-law",
            areasOfLaw
        );

        assertThat(areasOfLawResponse.statusCode())
            .as("Expected 201 CREATED when setting areas of law for service centre %s", serviceCentreId)
            .isEqualTo(CREATED.value());

        return serviceCentreId;
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/courts/name-prefix/" + TEST_COURT_PREFIX);
        http.doDelete("/testing-support/service-centres/name-prefix/" + TEST_SERVICE_CENTRE_PREFIX);
    }
}
