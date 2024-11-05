package com.jsh.erp.service.cache;

import org.ehcache.expiry.ExpiryPolicy;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

public class CustomExpiryPolicy implements ExpiryPolicy<String, String> {

    @Override
    public Duration getExpiryForCreation(String key, String value) {
        // 根据key或其他条件设置过期时间
        if ("key1".equals(key)) {
            return Duration.of(10, ChronoUnit.SECONDS);
        } else if ("key2".equals(key)) {
            return Duration.of(1, ChronoUnit.MINUTES);
        }
        return Duration.ZERO; // 默认不缓存
    }

    @Override
    public Duration getExpiryForAccess(String key, Supplier<? extends String> value) {
        return Duration.ZERO;
    }

    @Override
    public Duration getExpiryForUpdate(String key, Supplier<? extends String> oldValue, String newValue) {
        return Duration.ZERO;
    }
}
