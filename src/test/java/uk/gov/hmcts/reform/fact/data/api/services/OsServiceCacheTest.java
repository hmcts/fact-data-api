package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import uk.gov.hmcts.reform.fact.data.api.config.CacheConfiguration;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsDpa;
import uk.gov.hmcts.reform.fact.data.api.os.OsFeignClient;
import uk.gov.hmcts.reform.fact.data.api.os.OsLocationData;
import uk.gov.hmcts.reform.fact.data.api.os.OsResult;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(OsServiceCacheTest.TestConfig.class)
@TestPropertySource(properties = {
    "os.cache.enabled=true",
    "os.cache.maximum-size=10",
    "os.cache.time-to-live-millis=300000"
})
class OsServiceCacheTest {

    @Autowired
    private OsService osService;

    @Autowired
    private OsFeignClient osFeignClient;

    @Autowired
    private LocalAuthorityTypeRepository localAuthorityTypeRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void resetState() {
        reset(osFeignClient, localAuthorityTypeRepository);
        Objects.requireNonNull(cacheManager.getCache(CacheConfiguration.OSDATA_CACHE_NAME)).clear();
    }

    @Test
    void shouldReuseCachedResultForEquivalentPostcodeCasing() {
        OsData osData = OsData.builder()
            .results(List.of(OsResult.builder().dpa(OsDpa.builder().lat(51.5).lng(-0.1).build()).build()))
            .build();
        when(osFeignClient.getOsPostcodeData("SW1A 1AA")).thenReturn(osData);

        OsData firstResult = osService.getOsAddressByFullPostcode("sw1a 1aa");
        OsData secondResult = osService.getOsAddressByFullPostcode("SW1A 1AA");

        assertThat(firstResult).isSameAs(secondResult);
        verify(osFeignClient).getOsPostcodeData("SW1A 1AA");
    }

    @Test
    void shouldReuseCachedPartialResultForPostcodesWithTheSameSearchPrefix() {
        OsData osData = OsData.builder()
            .results(List.of(OsResult.builder().dpa(OsDpa.builder()
                .lat(51.5)
                .lng(-0.1)
                .localCustodianCode(123)
                .build()).build()))
            .build();
        when(osFeignClient.getOsPostcodeDataWithMaxResultsLimit("SW1A 1", 1)).thenReturn(osData);
        when(localAuthorityTypeRepository.findParentOrChildNameByCustodianCode(123))
            .thenReturn(Optional.of(LocalAuthorityType.builder().name("Test Authority").build()));

        OsLocationData firstResult = osService.getOsLonLatDistrictByPartial("sw1a 1aa");
        OsLocationData secondResult = osService.getOsLonLatDistrictByPartial("SW1A1AB");

        assertThat(firstResult).isSameAs(secondResult);
        verify(osFeignClient).getOsPostcodeDataWithMaxResultsLimit("SW1A 1", 1);
    }

    @TestConfiguration
    @Import(CacheConfiguration.class)
    static class TestConfig {

        @Bean
        OsFeignClient osFeignClient() {
            return mock(OsFeignClient.class);
        }

        @Bean
        LocalAuthorityTypeRepository localAuthorityTypeRepository() {
            return mock(LocalAuthorityTypeRepository.class);
        }

        @Bean
        OsService osService(
            OsFeignClient client,
            LocalAuthorityTypeRepository repository,
            ObjectProvider<OsService> osServiceProvider
        ) {
            return new OsService(client, repository, osServiceProvider);
        }
    }
}
