/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.cache;

import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.MapValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.character.StrKit;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据缓存
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/26 , Version 1.0.0
 */
public class DataCache {

    private DataCache() {

    }

    private static final Map<String, TableDataCache> CACHE_MAP = new ConcurrentHashMap<>(16);

    public static TableDataCache getOrCreate(String tableName) {
        return CACHE_MAP.computeIfAbsent(tableName, k -> new TableDataCache());
    }

    public static void set(String tableName, TableDataCache tableDataCache) {
        CACHE_MAP.put(Objects.requireNonNull(tableName), Objects.requireNonNull(tableDataCache));
    }

    public static final class TableDataCache {

        private final Map<String, Value> dataMap = new ConcurrentHashMap<>(16);

        public TableDataCache set(String key, Value value) {
            dataMap.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
            return this;
        }

        public Value get(String key) {
            return dataMap.get(key);
        }

        public TableDataCache remove(String key) {
            if (StrKit.isNotEmpty(key)) {
                dataMap.remove(key);
            }
            return this;
        }

        public TableDataCache remove(String key, Value value) {
            if (Objects.isNull(value)) {
                return remove(key);
            }
            Value oldValue = dataMap.get(key);
            if (oldValue instanceof ListValue lv) {
                // 列表类型
                if (value instanceof ListValue lvr) {
                    lv.removeAll(lvr);
                } else {
                    lv.remove(value);
                }
            } else {
                dataMap.remove(key);
            }
            return this;
        }

        public boolean isEmpty() {
            return dataMap.isEmpty();
        }

        public MapValue toMapValue() {
            return new MapValue(dataMap);
        }
    }
}
