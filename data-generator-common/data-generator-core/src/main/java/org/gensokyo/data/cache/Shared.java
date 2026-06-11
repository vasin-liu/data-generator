/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.cache;


import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 共享缓存
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/5 , Version 1.0.0
 */
public class Shared {

    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>(32);

    public static void put(String key, Object value) {
        CACHE.put(key, value);
    }

    public static void remove(String key) {
        CACHE.remove(key);
    }

    public static void removeAll() {
        CACHE.clear();
    }

    public static Object get(String key) {
        return CACHE.get(key);
    }

    public static Date getAsDate(String key) {
        return (Date) CACHE.get(key);
    }

    public static String getAsString(String key) {
        return (String) CACHE.get(key);
    }

    public static Long getAsLong(String key) {
        return (Long) CACHE.get(key);
    }

    public static Integer getAsInteger(String key) {
        return (Integer) CACHE.get(key);
    }
}
