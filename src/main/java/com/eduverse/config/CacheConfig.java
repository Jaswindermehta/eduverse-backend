package com.eduverse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * REDIS SPRING CACHING SYSTEM CONFIGURATION
 * ============================================================================
 * 
 * This class configures the caching engine for the Eduverse backend, utilizing
 * Redis as the high-performance in-memory cache-aside data store.
 * 
 * Key Configurations:
 * 1. @EnableCaching: Instructs Spring Boot to scan for caching annotations.
 * 2. Custom Serializers: Serializes cache keys as plain Strings, and cache
 *    values as structured JSON payloads (via Jackson) instead of raw Java binary.
 * 3. Granular Time-To-Live (TTL): Customizes cache expirations (e.g. 10 minutes
 *    for course catalogs, 30 minutes for category layouts) to avoid stale data.
 */
@Configuration
@EnableCaching // Enables Spring Boot's caching capabilities
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 1. Establish the default, baseline cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // Default TTL is 10 minutes
                .disableCachingNullValues() // Never cache null results
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // 2. Define custom TTL settings for individual cache spaces
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Course lists: 10 minutes TTL
        cacheConfigurations.put("courses", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Detailed individual course lookups: 10 minutes TTL
        cacheConfigurations.put("courseDetails", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Categories listings (highly static): 30 minutes TTL
        cacheConfigurations.put("categories", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // 3. Instantiate and build the RedisCacheManager
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig) // Register base configurations
                .withInitialCacheConfigurations(cacheConfigurations) // Apply custom overrides
                .transactionAware() // Enforce transactional cache writes / evictions
                .build();
    }
}
