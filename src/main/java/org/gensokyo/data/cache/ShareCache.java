/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 共享缓存
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/5 , Version 1.0.0
 */
public class ShareCache {

    private static final Cache<String, Object> CACHE = CacheBuilder.newBuilder()
            .initialCapacity(5)
            .maximumSize(100)
            .concurrencyLevel(3)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    public static void put(String key, Object value) {
        CACHE.put(key, value);
    }

    public static void remove(String key) {
        CACHE.invalidate(key);
    }

    public static void removeAll() {
        CACHE.invalidateAll();
    }

    public static Object get(String key) {
        return CACHE.getIfPresent(key);
    }

    public static Date getAsDate(String key) {
        return (Date) CACHE.getIfPresent(key);
    }

    public static String getAsString(String key) {
        return (String) CACHE.getIfPresent(key);
    }

    public static Long getAsLong(String key) {
        return (Long) CACHE.getIfPresent(key);
    }

    public static Integer getAsInteger(String key) {
        return (Integer) CACHE.getIfPresent(key);
    }
}
