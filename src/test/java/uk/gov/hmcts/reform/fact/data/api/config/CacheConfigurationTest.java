package uk.gov.hmcts.reform.fact.data.api.config;

import com.github.benmanes.caffeine.cache.Policy;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CacheConfigurationTest {

    @Test
    void shouldCreateCaffeineCacheManagerWithOsDataCacheRegistered() {
        CacheConfiguration configuration = new CacheConfiguration(25, Duration.ofMinutes(5).toMillis());

        CacheManager cacheManager = configuration.cacheManager();

        assertNotNull(cacheManager);
        assertInstanceOf(CaffeineCacheManager.class, cacheManager);

        Cache osDataCache = cacheManager.getCache(CacheConfiguration.OSDATA_CACHE_NAME);
        assertNotNull(osDataCache);

        // Basic behavior check to ensure the cache is usable
        osDataCache.put("key", "value");
        Cache.ValueWrapper wrapper = osDataCache.get("key");
        assertNotNull(wrapper);
        Object value = wrapper.get();
        assertEquals("value", value);

        CaffeineCache caffeineCache = assertInstanceOf(CaffeineCache.class, osDataCache);
        Policy<Object, Object> policy = caffeineCache.getNativeCache().policy();
        assertEquals(25, policy.eviction().orElseThrow().getMaximum());
        assertEquals(
            Duration.ofMinutes(5).toNanos(),
            policy.expireAfterWrite().orElseThrow().getExpiresAfter(TimeUnit.NANOSECONDS)
        );
    }
}
