package com.ecommerce.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@EnableCaching
@Configuration
public class CaffeineCacheConfig {

    private static final String POPULAR_PRODUCTS_CACHE = "product:popular";
    private static final String PRODUCT_DETAIL_CACHE = "product:detail";
    private static final int CACHE_MAX_SIZE = 10;
    private static final int CACHE_TTL_SECONDS = 30;

    /**
     * Caffeine 기반 CacheManager
     *
     * @Primary: Redis CacheManager보다 우선 적용
     */
    @Primary
    @Bean
    public CacheManager caffeineCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        // 인기 상품 캐시 설정
        CaffeineCache popularProductsCache = new CaffeineCache(
                POPULAR_PRODUCTS_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(CACHE_MAX_SIZE)
                        .expireAfterWrite(CACHE_TTL_SECONDS, TimeUnit.SECONDS)
                        .recordStats()
                        .build()
        );

        CaffeineCache productDetailCache = new CaffeineCache(
                PRODUCT_DETAIL_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(1_000)
                        .expireAfterWrite(CACHE_TTL_SECONDS, TimeUnit.SECONDS)
                        .build()
        );

        cacheManager.setCaches(List.of(
                popularProductsCache,
                productDetailCache
        ));
        log.info("로컬 캐시 초기화 완료 - cache: {}, TTL: {}초",
                POPULAR_PRODUCTS_CACHE, CACHE_TTL_SECONDS);
        log.info("로컬 캐시 초기화 완료 - cache: {}, TTL: {}초",
                PRODUCT_DETAIL_CACHE, CACHE_TTL_SECONDS);

        return cacheManager;
    }
}