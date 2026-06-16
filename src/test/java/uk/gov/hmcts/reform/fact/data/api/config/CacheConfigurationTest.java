package uk.gov.hmcts.reform.fact.data.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CacheConfigurationTest {

    @Test
    void shouldCreateCaffeineCacheManagerWithOsDataCacheRegistered() {
        CacheConfiguration configuration = new CacheConfiguration();

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
    }
}
