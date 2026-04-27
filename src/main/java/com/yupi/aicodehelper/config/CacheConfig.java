package com.yupi.aicodehelper.config;

import com.yupi.aicodehelper.config.properties.AppCacheProperties;
import com.yupi.aicodehelper.hot100.Hot100CacheNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig extends CachingConfigurerSupport {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory,
                                     AppCacheProperties appCacheProperties) {
        if (!appCacheProperties.isRedisEnabled()) {
            log.info("Redis cache disabled by config, fallback to local in-memory cache manager");
            return localCacheManager();
        }
        if (!isRedisReachable(redisConnectionFactory)) {
            log.warn("Redis is unreachable, fallback to local in-memory cache manager");
            return localCacheManager();
        }

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(Hot100CacheNames.PROBLEM_LIST,
                defaultConfig.entryTtl(Duration.ofSeconds(appCacheProperties.getHot100ProblemTtlSeconds())));
        cacheConfigs.put(Hot100CacheNames.PROBLEM_DETAIL,
                defaultConfig.entryTtl(Duration.ofSeconds(appCacheProperties.getHot100ProblemTtlSeconds())));
        cacheConfigs.put(Hot100CacheNames.TAG_LIST,
                defaultConfig.entryTtl(Duration.ofSeconds(appCacheProperties.getHot100ProblemTtlSeconds())));
        cacheConfigs.put(Hot100CacheNames.RECOMMENDATION,
                defaultConfig.entryTtl(Duration.ofSeconds(appCacheProperties.getHot100RecommendationTtlSeconds())));
        cacheConfigs.put(Hot100CacheNames.STUDY_PLAN,
                defaultConfig.entryTtl(Duration.ofSeconds(appCacheProperties.getHot100StudyPlanTtlSeconds())));
        cacheConfigs.put(Hot100CacheNames.TAG_MASTERY,
                defaultConfig.entryTtl(Duration.ofSeconds(appCacheProperties.getHot100TagMasteryTtlSeconds())));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache get error, cache={}, key={}", cache == null ? "unknown" : cache.getName(), key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Cache put error, cache={}, key={}", cache == null ? "unknown" : cache.getName(), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache evict error, cache={}, key={}", cache == null ? "unknown" : cache.getName(), key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("Cache clear error, cache={}", cache == null ? "unknown" : cache.getName(), exception);
            }
        };
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return cacheErrorHandler();
    }

    private GenericJackson2JsonRedisSerializer redisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }

    private CacheManager localCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                Hot100CacheNames.PROBLEM_LIST,
                Hot100CacheNames.PROBLEM_DETAIL,
                Hot100CacheNames.TAG_LIST,
                Hot100CacheNames.RECOMMENDATION,
                Hot100CacheNames.STUDY_PLAN,
                Hot100CacheNames.TAG_MASTERY
        );
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    private boolean isRedisReachable(RedisConnectionFactory redisConnectionFactory) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.ping();
            return true;
        } catch (Exception e) {
            log.warn("Redis connectivity check failed: {}", e.getMessage());
            return false;
        }
    }
}
