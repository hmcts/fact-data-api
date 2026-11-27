package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.dto.ServiceCentreWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.PostcodeReference;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.types.ServiceAreaType;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsDpa;
import uk.gov.hmcts.reform.fact.data.api.os.OsFeignClient;
import uk.gov.hmcts.reform.fact.data.api.os.OsResult;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAddressRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.PostcodeReferenceRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreRepository;
import uk.gov.hmcts.reform.fact.data.api.services.AllLocationService;
import uk.gov.hmcts.reform.fact.data.api.services.CourtAddressService;
import uk.gov.hmcts.reform.fact.data.api.services.CourtSinglePointOfEntryService;
import uk.gov.hmcts.reform.fact.data.api.services.OsService;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceAreaService;
import uk.gov.hmcts.reform.fact.data.api.services.search.SearchCourtService;
import uk.gov.hmcts.reform.fact.data.api.services.search.SearchExecuter;
import uk.gov.hmcts.reform.fact.data.api.services.search.SearchLocationService;
import uk.gov.hmcts.reform.fact.data.api.services.search.SearchServiceCentreService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType.LOCAL;
import static uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType.REGIONAL;

@WebMvcTest({SearchCourtController.class, SearchLocationController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({
    OsService.class, SearchCourtService.class, SearchLocationService.class,
    SearchServiceCentreService.class, SearchExecuter.class
})
class TerminatedPostcodeSearchTest {

    private static final String COURTS_URL = "/search/courts/v1/postcode";
    private static final String LOCATIONS_URL = "/search/locations/v1/postcode";
    private static final String POSTCODE = "B16 6TZ";
    private static final double LATITUDE = 52.473;
    private static final double LONGITUDE = -1.93;
    private static final UUID COURT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID CENTRE_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
    private static final UUID SERVICE_AREA_ID = UUID.fromString("323e4567-e89b-12d3-a456-426614174000");
    private static final UUID AREA_OF_LAW_ID = UUID.fromString("423e4567-e89b-12d3-a456-426614174000");
    private static final CourtWithDistance COURT = courtResult();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OsFeignClient osFeignClient;

    @MockitoBean
    private PostcodeReferenceRepository postcodeReferenceRepository;

    @MockitoBean
    private LocalAuthorityTypeRepository localAuthorityTypeRepository;

    @MockitoBean
    private CourtAddressRepository courtAddressRepository;

    @MockitoBean
    private ServiceCentreRepository serviceCentreRepository;

    @MockitoBean
    private CourtAddressService courtAddressService;

    @MockitoBean
    private CourtSinglePointOfEntryService courtSinglePointOfEntryService;

    @MockitoBean
    private ServiceAreaService serviceAreaService;

    @MockitoBean
    private AllLocationService allLocationService;

    @ParameterizedTest
    @CsvSource({
        COURTS_URL + ", true",
        COURTS_URL + ", false",
        LOCATIONS_URL + ", true",
        LOCATIONS_URL + ", false"
    })
    void shouldFindNearestCourtsUsingTerminatedCoordinates(String url, boolean largeUser) throws Exception {
        when(postcodeReferenceRepository.findById(POSTCODE))
            .thenReturn(Optional.of(terminatedReference(largeUser, true)));
        when(osFeignClient.getOsPostcodeData(POSTCODE))
            .thenReturn(OsData.builder().results(List.of()).build());
        when(courtAddressService.findCourtWithDistanceByOsData(LATITUDE, LONGITUDE, 10))
            .thenReturn(List.of(COURT));

        var response = mockMvc.perform(get(url).param("postcode", "b16 6tz"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        if (COURTS_URL.equals(url)) {
            response.andExpect(jsonPath("$[0].courtId").value(COURT_ID.toString()))
                .andExpect(jsonPath("$[0].courtName").value("Test Court"))
                .andExpect(jsonPath("$[0].courtSlug").value("test-court"));
        } else {
            response.andExpect(jsonPath("$[0].id").value(COURT_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Test Court"))
                .andExpect(jsonPath("$[0].slug").value("test-court"))
                .andExpect(jsonPath("$[0].type").value("COURT"));
        }
        response.andExpect(jsonPath("$[0].distance").value(2.5));
        verify(courtAddressService).findCourtWithDistanceByOsData(LATITUDE, LONGITUDE, 10);
    }

    @ParameterizedTest
    @CsvSource({
        COURTS_URL + ", true",
        COURTS_URL + ", false",
        LOCATIONS_URL + ", true",
        LOCATIONS_URL + ", false"
    })
    void shouldFindServiceAreaResultsUsingTerminatedCoordinates(String url, boolean largeUser) throws Exception {
        when(postcodeReferenceRepository.findById(POSTCODE))
            .thenReturn(Optional.of(terminatedReference(largeUser, true)));
        when(osFeignClient.getOsPostcodeDataWithMaxResultsLimit("B16 6", 1))
            .thenReturn(OsData.builder().results(List.of(
                OsResult.builder().dpa(OsDpa.builder()
                    .lat(52.48).lng(-1.94).localCustodianCode(123).build()).build()
            )).build());
        when(localAuthorityTypeRepository.findParentOrChildNameByCustodianCode(123))
            .thenReturn(Optional.of(LocalAuthorityType.builder().name("Test Authority").build()));
        when(serviceAreaService.getServiceAreaByName("Crime"))
            .thenReturn(ServiceArea.builder()
                .id(SERVICE_AREA_ID).areaOfLawId(AREA_OF_LAW_ID).type(ServiceAreaType.OTHER).build());
        when(courtAddressRepository.findNearestByAreaOfLaw(LATITUDE, LONGITUDE, AREA_OF_LAW_ID, 10))
            .thenReturn(List.of(COURT));
        if (LOCATIONS_URL.equals(url)) {
            when(serviceCentreRepository.findNearestByServiceAreaAndAreaOfLawAndCatchmentTypeIn(
                SERVICE_AREA_ID, AREA_OF_LAW_ID, List.of(LOCAL, REGIONAL), LATITUDE, LONGITUDE, 10
            )).thenReturn(List.of(centreResult()));
        }

        var response = mockMvc.perform(get(url)
                                           .param("postcode", POSTCODE)
                                           .param("serviceArea", "Crime")
                                           .param("action", "nearest"))
            .andExpect(status().isOk());

        if (COURTS_URL.equals(url)) {
            response.andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].courtId").value(COURT_ID.toString()))
                .andExpect(jsonPath("$[0].distance").value(2.5));
        } else {
            response.andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(CENTRE_ID.toString()))
                .andExpect(jsonPath("$[0].type").value("SERVICE_CENTRE"))
                .andExpect(jsonPath("$[0].distance").value(1.5))
                .andExpect(jsonPath("$[1].id").value(COURT_ID.toString()))
                .andExpect(jsonPath("$[1].type").value("COURT"))
                .andExpect(jsonPath("$[1].distance").value(2.5));
            verify(serviceCentreRepository).findNearestByServiceAreaAndAreaOfLawAndCatchmentTypeIn(
                SERVICE_AREA_ID, AREA_OF_LAW_ID, List.of(LOCAL, REGIONAL), LATITUDE, LONGITUDE, 10
            );
        }
        verify(courtAddressRepository).findNearestByAreaOfLaw(LATITUDE, LONGITUDE, AREA_OF_LAW_ID, 10);
    }

    @ParameterizedTest
    @CsvSource({
        COURTS_URL + ", false",
        COURTS_URL + ", true",
        LOCATIONS_URL + ", false",
        LOCATIONS_URL + ", true"
    })
    void shouldReturnBadRequestWhenNeitherSourceResolvesPostcode(String url, boolean serviceArea) throws Exception {
        when(postcodeReferenceRepository.findById(POSTCODE))
            .thenReturn(Optional.of(terminatedReference(false, false)));
        var request = get(url).param("postcode", POSTCODE);
        if (serviceArea) {
            request.param("serviceArea", "Crime").param("action", "nearest");
            when(osFeignClient.getOsPostcodeDataWithMaxResultsLimit("B16 6", 1))
                .thenReturn(OsData.builder().results(List.of()).build());
        } else {
            when(osFeignClient.getOsPostcodeData(POSTCODE))
                .thenReturn(OsData.builder().results(List.of()).build());
        }

        mockMvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("No address results returned from OS")));

        verifyNoInteractions(courtAddressRepository, courtAddressService, serviceCentreRepository);
    }

    private PostcodeReference terminatedReference(boolean largeUser, boolean coordinatesAvailable) {
        return PostcodeReference.builder()
            .postcode(POSTCODE)
            .status("terminated")
            .largeUser(largeUser)
            .introducedYearMonth("198001")
            .terminatedYearMonth("201804")
            .coordinateQuality(coordinatesAvailable ? 1 : 9)
            .coordinatesAvailable(coordinatesAvailable)
            .latitude(coordinatesAvailable ? LATITUDE : null)
            .longitude(coordinatesAvailable ? LONGITUDE : null)
            .build();
    }

    private static CourtWithDistance courtResult() {
        return new CourtWithDistance() {
            @Override
            public UUID getCourtId() {
                return COURT_ID;
            }

            @Override
            public String getCourtName() {
                return "Test Court";
            }

            @Override
            public String getCourtSlug() {
                return "test-court";
            }

            @Override
            public BigDecimal getDistance() {
                return BigDecimal.valueOf(2.5);
            }
        };
    }

    private static ServiceCentreWithDistance centreResult() {
        return new ServiceCentreWithDistance() {
            @Override
            public UUID getServiceCentreId() {
                return CENTRE_ID;
            }

            @Override
            public String getServiceCentreName() {
                return "Test Centre";
            }

            @Override
            public String getServiceCentreSlug() {
                return "test-centre";
            }

            @Override
            public BigDecimal getDistance() {
                return BigDecimal.valueOf(1.5);
            }
        };
    }
}
