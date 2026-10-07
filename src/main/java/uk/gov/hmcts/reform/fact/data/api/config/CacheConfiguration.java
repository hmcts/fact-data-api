package uk.gov.hmcts.reform.fact.data.api.config;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.fact.data.api.config.properties.OsConfigurationProperties;

@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "os.cache", name = "enabled", havingValue = "true")
public class CacheConfiguration {

    public static final String OSDATA_CACHE_NAME = "osdata";

    private final long maximumSize;
    private final long timeToLiveMillis;

    public CacheConfiguration(OsConfigurationProperties osConfigurationProperties) {
        this.maximumSize = osConfigurationProperties.getCache().getMaximumSize();
        this.timeToLiveMillis = osConfigurationProperties.getCache().getTimeToLiveMillis();
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new
            CaffeineCacheManager();
        cacheManager.registerCustomCache(OSDATA_CACHE_NAME, buildOsDataCache());
        return cacheManager;
    }

    private Cache<Object, Object> buildOsDataCache() {
        return Caffeine.newBuilder()
            .initialCapacity(10)
            .maximumSize(maximumSize)
            .expireAfterWrite(Duration.ofMillis(timeToLiveMillis))
            .scheduler(Scheduler.systemScheduler())
            .build();
    }
}
