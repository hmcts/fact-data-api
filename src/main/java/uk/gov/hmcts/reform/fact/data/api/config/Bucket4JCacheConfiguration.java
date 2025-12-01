package uk.gov.hmcts.reform.fact.data.api.config;

import static uk.gov.hmcts.reform.fact.data.api.config.Bucket4JConfiguration.CACHE_NAME;

import javax.cache.Cache;
import javax.cache.CacheManager;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.grid.jcache.JCacheProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration("redisConfiguration")
@Slf4j
@EnableCaching
public class Bucket4JCacheConfiguration {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private String redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Bean
    // rldev is just for this rate limit test; It lets me specifically
    // target the cluster config for testing without needing ssl enabled
    @Profile({"!dev & !test & !rldev"})
    public Config redissonConfiguration() {
        log.warn("Using PROD redisson configuration");
        String connectionString = "rediss://" + (redisPassword.isEmpty() ? "" : ":" + redisPassword + "@")
            + redisHost + ":" + redisPort;
        Config config = new Config();
        config.useClusterServers().addNodeAddress(connectionString);
        return config;
    }

    @Bean
    @Profile({"rldev"})
    public Config redissonConfigurationRlDev() {
        log.warn("Using RLDEV redisson configuration");
        String connectionString = "redis://" + (redisPassword.isEmpty() ? "" : ":" + redisPassword + "@")
            + redisHost + ":" + redisPort;
        Config config = new Config();
        config.useClusterServers().addNodeAddress(connectionString);
        return config;
    }

    @Bean
    @Profile({"dev", "test"})
    public Config redissonConfigurationDev() {
        log.warn("Using DEV/TEST redisson configuration");
        // in production, use "rediss://"
        String connectionString = "redis://" + (redisPassword.isEmpty() ? "" : ":" + redisPassword + "@")
            + redisHost + ":" + redisPort;
        Config config = new Config();
        // in production, use config.useClusterServer().addNodeAddress()
        config.useSingleServer().setAddress(connectionString);
        return config;
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "spring.cache.jcache",
        name = "provider",
        havingValue = "org.redisson.jcache.JCachingProvider",
        matchIfMissing = false)
    public RedissonClient redissonClient(Config redissonConfig) {
        return Redisson.create(redissonConfig);
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "spring.cache.jcache",
        name = "provider",
        havingValue = "org.redisson.jcache.JCachingProvider",
        matchIfMissing = false)
    public ProxyManager<String> proxyManagerRedis(CacheManager cacheManager, Config redissonConfig) {
        try {
            cacheManager.createCache(
                CACHE_NAME,
                RedissonConfiguration.fromConfig(redissonConfig)
            );
        } catch (Exception e) {
            log.warn("Failed to connect to redis: {}", e.getMessage());
        }
        return new JCacheProxyManager<>(cacheManager.getCache(CACHE_NAME));
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "spring.cache.jcache",
        name = "provider",
        havingValue = "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider",
        matchIfMissing = false)
    public ProxyManager<String> proxyManagerCaffeine(CacheManager cacheManager) {
        com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration<String, byte[]>
            caffeineConfiguration = new com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration<>();
        Cache<String, byte[]> cache = cacheManager.createCache(CACHE_NAME, caffeineConfiguration);
        return new JCacheProxyManager<>(cache);
    }
}


