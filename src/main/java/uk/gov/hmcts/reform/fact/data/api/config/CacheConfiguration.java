package uk.gov.hmcts.reform.fact.data.api.config;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "testingSupport", name = "enableCache", havingValue = "true")
@Slf4j
public class CacheConfiguration {

    public static final String OSDATA_CACHE_NAME = "osdata";

    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new
            CaffeineCacheManager();
        cacheManager.registerCustomCache(OSDATA_CACHE_NAME, buildOsDataCache());
        return cacheManager;
    }

    private Cache<Object, Object> buildOsDataCache() {
        Cache<Object, Object> cache = Caffeine.newBuilder()
            .initialCapacity(10)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats()
            .scheduler(Scheduler.systemScheduler())
            .build();

        executorService.scheduleWithFixedDelay(() -> log.info("OsData Cache stats: {}", cache.stats()),
                                               0, 5, TimeUnit.MINUTES);

        return cache;
    }
}
