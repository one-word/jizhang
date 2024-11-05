package com.jsh.erp.service.redis;

import com.jsh.erp.utils.StringUtil;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;

@Component
public class RedisService {

    private final Cache<String, Object> cache;
    private final Set<String> trackedKeys; // 用于跟踪键

    public RedisService() {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
            .withCache("myCache",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Object.class,
                    ResourcePoolsBuilder.newResourcePoolsBuilder().heap(100, EntryUnit.ENTRIES)))
            .build(true);
        cache = cacheManager.getCache("myCache", String.class, Object.class);
        trackedKeys = new HashSet<>();
    }

    public static final String ACCESS_TOKEN = "X-Access-Token";

    public Object getObjectFromSessionByKey(HttpServletRequest request, String key) {
        Object obj = null;
        if (request == null) {
            return null;
        }
        String token = request.getHeader(ACCESS_TOKEN);
        if (token != null) {
            // 从缓存中获取
            obj = cache.get(token + ":" + key);
        }
        return obj;
    }

    public <T> T getCacheObject(final String key) {
        return (T)cache.get(key);
    }

    public void storageObjectBySession(String token, String key, Object obj) {
        // 存储到缓存中
        cache.put(token + ":" + key, obj);
        trackedKeys.add(token + ":" + key);
    }

    public void storageCaptchaObject(String verifyKey, String codeNum) {
        cache.put(verifyKey, codeNum);
        // 需要实现过期逻辑，Ehcache 默认不支持 per-key 过期
    }

    public boolean deleteObject(final String key) {
        cache.remove(key);
        return true;
    }

    public void deleteObjectBySession(HttpServletRequest request, String key) {
        if (request != null) {
            String token = request.getHeader(ACCESS_TOKEN);
            if (StringUtil.isNotEmpty(token)) {
                // 从缓存中删除
                cache.remove(token + ":" + key);
                trackedKeys.remove(token + ":" + key); // 移除跟踪
            }
        }
    }

    public void deleteObjectByUserAndIp(Long userId, String clientIp) {
        Set<String> keysToDelete = new HashSet<>();

        // 遍历跟踪的键
        for (String key : trackedKeys) {
            Object userIdValue = cache.get(key + ":userId");
            Object clientIpValue = cache.get(key + ":clientIp");
            if (userIdValue != null && clientIpValue != null &&
                userIdValue.equals(userId.toString()) && clientIpValue.equals(clientIp)) {
                keysToDelete.add(key);
            }
        }

        // 删除符合条件的键
        for (String key : keysToDelete) {
            cache.remove(key);
            trackedKeys.remove(key); // 更新跟踪
        }
    }
}
