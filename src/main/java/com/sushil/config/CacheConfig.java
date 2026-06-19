package com.sushil.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String USERS        = "users";
    public static final String USER_BY_NAME = "userByName";
    public static final String TOKEN_ACTIVE = "tokenActive";
    public static final String CHAIN_STATS  = "chainStats";
    public static final String BLOCK_BY_IDX = "blockByIndex";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
            build(USERS,        500,  120),
            build(USER_BY_NAME, 5000, 300),
            build(TOKEN_ACTIVE, 50000, 60),
            build(CHAIN_STATS,  1,    30),
            build(BLOCK_BY_IDX, 1000, 600)
        ));
        return manager;
    }

    private CaffeineCache build(String name, long maxSize, long ttlSeconds) {
        return new CaffeineCache(name,
            Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .recordStats()
                .build());
    }
}
