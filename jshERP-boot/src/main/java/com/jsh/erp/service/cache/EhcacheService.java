package com.jsh.erp.service.cache;

import com.google.common.collect.Lists;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheRuntimeConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventType;
import org.ehcache.expiry.ExpiryPolicy;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 缓存服务类
 */
@Service
public class EhcacheService {

    protected static final CacheManager CACHE_MANAGER = CacheManagerBuilder.newCacheManagerBuilder().build(true);

    /**
     * 默认缓存容器名
     */
    protected final String DEFAULT_CACHE_NAME = "DEFAULT_CACHE";

    public EhcacheService() {
        //默认缓存容器创建
        createCacheIfNot(DEFAULT_CACHE_NAME);
    }

    public Cache<? super Serializable, ? super Serializable> getCache(String cacheName) {
        return CACHE_MANAGER.getCache(cacheName, Serializable.class, Serializable.class);
    }

    /**
     * 线程安全的创建缓存容器
     *
     * @param cacheName 缓存容器名字
     * @return
     */
    public Cache<? super Serializable, ? super Serializable> createCache(String cacheName) {
        Cache<? super Serializable, ? super Serializable> cache = getCache(cacheName);
        if (cache == null) {
            synchronized (this.getClass()) {
                cache = getCache(cacheName);
                if (cache == null) {
                    //缓存过期策略
                    CustomExpiryPolicy<Object, Object> customExpiryPolicy = new CustomExpiryPolicy<>();

                    return CACHE_MANAGER.createCache(cacheName, CacheConfigurationBuilder.newCacheConfigurationBuilder(
                            Serializable.class,
                            Serializable.class,
                            ResourcePoolsBuilder.heap(Long.MAX_VALUE))
                        .withExpiry(customExpiryPolicy));
                } else {
                    return cache;
                }
            }
        } else {
            return cache;
        }
    }

    /**
     * 如果不存在则创建缓存对象
     *
     * @param cacheName 缓存名
     * @return 缓存对象
     */
    public Cache<? super Serializable, ? super Serializable> createCacheIfNot(String cacheName) {
        Cache<? super Serializable, ? super Serializable> cache = getCache(cacheName);
        return cache != null ? cache : createCache(cacheName);
    }

    /**
     * 获取键值
     *
     * @param cacheName 缓存容器名
     * @param key       键名
     */
    public <K extends Serializable> Serializable get(String cacheName, K key) {
        if (key == null) {
            return null;
        }
        cacheName = StringUtils.isEmpty(cacheName) ? DEFAULT_CACHE_NAME : cacheName;
        return (Serializable)(createCacheIfNot(cacheName).get(key));
    }

    /**
     * @param key 键名
     */
    public <K extends Serializable> Serializable get(K key) {
        return get(DEFAULT_CACHE_NAME, key);
    }

    public List<? extends Cache.Entry<? super Serializable, ? super Serializable>> getAllEntry(
        String cacheName) {
        cacheName = StringUtils.isEmpty(cacheName) ? DEFAULT_CACHE_NAME : cacheName;
        Cache<? super Serializable, ? super Serializable> cache = getCache(cacheName);
        return Lists.newArrayList(cache);
    }

    public List<? extends Cache.Entry<? super Serializable, ? super Serializable>> getAllEntry() {
        return getAllEntry(DEFAULT_CACHE_NAME);
    }

    /**
     * 获取容器{cacheName}所有键值
     *
     * @param cacheName
     * @return
     */
    public List<? super Serializable> getAllValues(String cacheName) {
        cacheName = StringUtils.isEmpty(cacheName) ? DEFAULT_CACHE_NAME : cacheName;
        return getAllEntry(cacheName).stream()
            .map(entry -> entry.getValue())
            .collect(Collectors.toList());
    }

    /**
     * 获取默认容器所有键值
     *
     * @return
     */
    public List<? super Serializable> getAllValues() {
        return getAllValues(DEFAULT_CACHE_NAME);
    }

    /**
     * 获取容器{cacheName}所有的键名
     *
     * @return
     */
    public List<? super Serializable> getAllKeys(String cacheName) {
        cacheName = StringUtils.isEmpty(cacheName) ? DEFAULT_CACHE_NAME : cacheName;
        return getAllEntry(cacheName).stream()
            .map(entry -> entry.getKey())
            .collect(Collectors.toList());
    }

    /**
     * 获取默认容器所有的键名
     *
     * @return
     */
    public List<? super Serializable> getAllKeys() {
        return getAllKeys(DEFAULT_CACHE_NAME);
    }

    /**
     * @param key 键名
     */
    public <K extends Serializable, V> V getByType(K key, Class<V> resultType) {
        Serializable result = get(DEFAULT_CACHE_NAME, key);
        return Optional.ofNullable(result)
            .map(elem -> new DefaultConversionService().convert(elem, resultType))
            .orElse(null);
    }

    /**
     * @param key 键名
     */
    public <K extends Serializable> String getStr(K key) {
        return getByType(key, String.class);
    }

    /**
     * 设置缓存
     *
     * @param key      键名
     * @param value    键值
     * @param expireMs 键值对过期时间（单位毫秒）- 不设置、或负数默认为永不过期
     */
    public <K extends Serializable, V extends Serializable> void set(String cacheName, K key, V value,
                                                                     Long expireMs) {
        cacheName = StringUtils.isEmpty(cacheName) ? DEFAULT_CACHE_NAME : cacheName;

        if (expireMs != null && expireMs >= 0) {
            CacheRuntimeConfiguration runtimeConfiguration = createCacheIfNot(cacheName).getRuntimeConfiguration();
            CustomExpiryPolicy expiryPolicy = (CustomExpiryPolicy)runtimeConfiguration.getExpiryPolicy();
            expiryPolicy.setExpire(key, Duration.ofMillis(expireMs));
        }

        createCacheIfNot(cacheName).put(key, value);
    }

    /**
     * @param key      键名
     * @param value    键值
     * @param expireMs 键值对过期时间（单位毫秒）- 不设置、或负数默认为永不过期
     */
    public <K extends Serializable, V extends Serializable> void set(String key, V value, Long expireMs) {
        set(DEFAULT_CACHE_NAME, key, value, expireMs);
    }

    /**
     * @param key   键名
     * @param value 键值
     */
    public <V extends Serializable> void set(String key, V value) {
        set(key, value, null);
    }

    /**
     * 删除某个容器的某个键值对
     *
     * @param cacheName 容器名
     * @param key       键名
     * @return
     */
    public Serializable del(String cacheName, Serializable key) {
        cacheName = StringUtils.isEmpty(cacheName) ? DEFAULT_CACHE_NAME : cacheName;
        Cache<? super Serializable, ? super Serializable> cache = getCache(cacheName);

        return Optional.ofNullable((Serializable)cache.get(key))
            .map(value -> {
                cache.remove(key);
                return value;
            })
            .orElse(null);
    }

    /**
     * 删除某人容器的某个键值对
     *
     * @param key 键名
     * @return
     */
    public Serializable del(Serializable key) {
        return del(DEFAULT_CACHE_NAME, key);
    }

    /**
     * 清空某个缓存容器的内容
     *
     * @param cacheName 缓存容器名
     */
    public void clear(String cacheName) {
        Cache<? super Serializable, ? super Serializable> cache = getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * 销毁全部容器
     */
    public void close() {
        CACHE_MANAGER.close();
    }

    public EhcacheService opsForHash() {
        return this;
    }

    public boolean hasKey(String token, String key) {
        return get(key).equals(token);
    }

    public void expire(String key, String token, Long maxSessionInSeconds, TimeUnit timeUnit) {
        set(key, token, timeUnit.toMillis(maxSessionInSeconds));
    }

    /**
     * keyValue自定义过期时间
     * <p>
     * 文档<a href="https://www.ehcache.org/documentation/3.10/expiry.html"></a>
     *
     * @param <K>
     * @param <V>
     */
    private class CustomExpiryPolicy<K, V> implements ExpiryPolicy<K, V> {

        private final ConcurrentHashMap<K, Duration> keyExpireMap = new ConcurrentHashMap();

        public Duration setExpire(K key, Duration duration) {
            return keyExpireMap.put(key, duration);
        }

        public Duration getExpireByKey(K key) {
            return Optional.ofNullable(keyExpireMap.get(key))
                .orElse(null);
        }

        public Duration removeExpire(K key) {
            return keyExpireMap.remove(key);
        }

        @Override
        public Duration getExpiryForCreation(K key, V value) {
            return Optional.ofNullable(getExpireByKey(key))
                .orElse(Duration.ofNanos(Long.MAX_VALUE));
        }

        @Override
        public Duration getExpiryForAccess(K key, Supplier<? extends V> value) {
            return getExpireByKey(key);
        }

        @Override
        public Duration getExpiryForUpdate(K key, Supplier<? extends V> oldValue, V newValue) {
            return getExpireByKey(key);
        }
    }

    /**
     * 自定义事件处理 == 当前主要是用于去除自定义键值对的Map过期时间东西，防止内存溢出
     * <p>
     * 文档<a href="https://www.ehcache.org/documentation/3.10/cache-event-listeners.html"></a>
     *
     * @param <K>
     * @param <V>
     */
    private static class CustomCacheEventListener<K, V> implements CacheEventListener<K, V> {

        String cacheName;

        Cache cache;

        CustomExpiryPolicy customExpiryPolicy;

        public CustomCacheEventListener(String cacheName) {
            this.cacheName = cacheName;
        }

        @Override
        public void onEvent(CacheEvent event) {
            this.cache =
                cache == null ? CACHE_MANAGER.getCache(cacheName, Serializable.class, Serializable.class) : cache;
            this.customExpiryPolicy = customExpiryPolicy == null ?
                (CustomExpiryPolicy)this.cache.getRuntimeConfiguration().getExpiryPolicy() : customExpiryPolicy;
            if (Arrays.stream(new String[] {
                EventType.EXPIRED.name(),
                EventType.EVICTED.name(),
                EventType.REMOVED.name()
            }).anyMatch(type -> type.equalsIgnoreCase(event.getType().name()))) {
                customExpiryPolicy.removeExpire(event.getKey());
            }
        }
    }
}
