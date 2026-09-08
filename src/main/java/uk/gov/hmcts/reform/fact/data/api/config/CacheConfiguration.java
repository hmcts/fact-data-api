package uk.gov.hmcts.reform.fact.data.api.config;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "os.cache", name = "enabled", havingValue = "true")
public class CacheConfiguration {

    public static final String OSDATA_CACHE_NAME = "osdata";

    private final long maximumSize;
    private final long timeToLiveMillis;

    public CacheConfiguration(
        @Value("${os.cache.maximum-size:1000}") long maximumSize,
        @Value("${os.cache.time-to-live-millis:3600000}") long timeToLiveMillis
    ) {
        this.maximumSize = maximumSize;
        this.timeToLiveMillis = timeToLiveMillis;
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
