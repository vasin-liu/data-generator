/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.cache;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
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
public class DataSet {

    private DataSet() {

    }

    /**
     * 模板实例数据缓存 ， 存放内容为：模板ID,实例ID,数据集
     */
    private static final Table<Long, Long, TableDataSet> CACHE = Tables.synchronizedTable(HashBasedTable.create());

    private static final String TEMPLATE_ID_NON_NULL = "模板编号不能为空";
    private static final String INSTANCE_ID_NON_NULL = "实例编号不能为空";

    public static TableDataSet getOrCreate(Long templateId, Long instanceId) {
        Objects.requireNonNull(templateId, TEMPLATE_ID_NON_NULL);
        Objects.requireNonNull(instanceId, INSTANCE_ID_NON_NULL);
        if (CACHE.contains(templateId, instanceId)) {
            return CACHE.get(templateId, instanceId);
        }
        var dataset = new TableDataSet();
        CACHE.put(templateId, instanceId, dataset);
        return CACHE.get(templateId, instanceId);
    }

    public static void set(Long templateId, Long instanceId, TableDataSet tableDataSet) {
        Objects.requireNonNull(templateId, TEMPLATE_ID_NON_NULL);
        Objects.requireNonNull(instanceId, INSTANCE_ID_NON_NULL);
        Objects.requireNonNull(tableDataSet, "数据集不能为空");
        CACHE.put(templateId, instanceId, tableDataSet);
    }

    public static void remove(Long templateId, Long instanceId) {
        Objects.requireNonNull(templateId, TEMPLATE_ID_NON_NULL);
        Objects.requireNonNull(instanceId, INSTANCE_ID_NON_NULL);
        CACHE.remove(templateId, instanceId);
    }

    public static final class TableDataSet {

        private final Map<String, Value> dataMap = new ConcurrentHashMap<>(32);

        public TableDataSet set(String key, Value value) {
            dataMap.put(Objects.requireNonNull(key, "数据集键名不能为空"),
                    Objects.requireNonNull(value, "数据集值不能为空"));
            return this;
        }

        public Value get(String key) {
            return dataMap.getOrDefault(key, null);
        }

        public Object getData(String key) {
            var val = get(key);
            return Objects.nonNull(val) ? val.get() : null;
        }

        public TableDataSet remove(String key) {
            if (StrKit.isNotEmpty(key)) {
                dataMap.remove(key);
            }
            return this;
        }

        public TableDataSet remove(String key, Value value) {
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
