package com.ecommerce.config;

import com.ecommerce.presentation.dto.product.PopularProductResponse;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final String CACHE_PREFIX = "ecommerce:cache:";
    private static final String POPULAR_PRODUCTS_CACHE = "product:popular";

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 인기 상품
        cacheConfigurations.put(POPULAR_PRODUCTS_CACHE,
                createCacheConfig(objectMapper, PopularProductResponse.class)
                        .entryTtl(Duration.ofMinutes(10)));

        // product: 단일 상품
//        cacheConfigurations.put("product",
//                createCacheConfig(objectMapper, ProductDetailDto.class)
//                        .entryTtl(Duration.ofHours(1)));

        // 기본 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper))
                )
                .computePrefixWith(cacheName -> CACHE_PREFIX + cacheName)
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    private <T> RedisCacheConfiguration createCacheConfig(
            ObjectMapper objectMapper, Class<T> type) {

        Jackson2JsonRedisSerializer<T> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, type);

        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(serializer)
                )
                .disableCachingNullValues();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // record
        mapper.registerModule(new com.fasterxml.jackson.module.paramnames.ParameterNamesModule());
        return mapper;
    }
}