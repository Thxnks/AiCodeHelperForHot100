package com.yupi.aicodehelper.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.yupi.aicodehelper.config.properties.AppCacheProperties;
import com.yupi.aicodehelper.hot100.Hot100CacheNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory,
                                     AppCacheProperties appCacheProperties) {
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

    private GenericJackson2JsonRedisSerializer redisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
