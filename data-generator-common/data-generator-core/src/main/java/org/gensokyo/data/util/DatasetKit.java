/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.util;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.MapValue;
import org.gensokyo.data.value.SingleValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.CollectKit;
import org.gensokyo.kit.collect.ListKit;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据集转换工具
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/6 , Version 1.0.0
 */
@Slf4j
public class DatasetKit {
    private DatasetKit() {
        throw new UnsupportedOperationException();
    }

    public static Value extractValue(Value input) {
        if (Objects.isNull(input)) {
            return Value.EMPTY;
        }

        if (input instanceof ListValue lv) {
            if (lv.isNullOrEmpty()) {
                return Value.EMPTY;
            }
            if (lv.size() == 1) {
                return lv.first();
            }
            return lv;
        }

        return input;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Value toValue(Object dataset) {
        if (Objects.isNull(dataset)) {
            return Value.EMPTY;
        }

        if (dataset instanceof Value val) {
            return extractValue(val);
        }

        if (dataset instanceof Collection<?> coll) {
            return extractCollection(coll);
        }

        if (dataset instanceof Map map) {
            return MapValue.fromMap(map);
        }

        return SingleValue.of(dataset);
    }

    @SuppressWarnings("unchecked")
    public static Value extractCollection(Collection<?> coll) {
        if (CollectKit.isEmpty(coll)) {
            return Value.EMPTY;
        }
        int size = coll.size();

        Class<?> type = CollectionUtils.findCommonElementType(coll);
        if (Objects.nonNull(type) && Value.class.isAssignableFrom(type)) {
            if (size > 1) {
                return ListValue.fromValueCollection(new ArrayList<>((Collection<? extends Value>) coll));
            } else {
                return SingleValue.of(coll.stream().findFirst().orElse(null));
            }
        }

        if (size > 1) {
            return ListValue.fromObjectCollection(new ArrayList<>(coll));
        } else {
            return SingleValue.of(coll.stream().findFirst().orElse(null));
        }
    }

    public static List<Object> toList(Object dataset) {
        var results = ListKit.newArrayList();
        if (Objects.isNull(dataset)) {
            return results;
        }
        if (dataset instanceof Collection<?> coll) {
            results.addAll(coll);
        } else if (dataset.getClass().isArray()) {
            results.addAll(ListKit.newArrayList(dataset));
        } else if (dataset instanceof Map<?, ?> map) {
            results.addAll(map.values().stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .stream()
                    .toList());
        } else {
            results.add(dataset);
        }
        return results;
    }

    public static InputStream buildBulkData(String template, List<Map<String, Object>> data) {
        return buildBulkData(template, data, Const.VERTICAL, Const.LF, Const.NULL);
    }

    public static InputStream buildBulkData(String template, List<Map<String, Object>> data, String columnDelimiter) {
        return buildBulkData(template, data, columnDelimiter, Const.LF, Const.NULL);
    }

    public static InputStream buildBulkData(String template, List<Map<String, Object>> data, String columnDelimiter,
                                            String nullValue) {
        return buildBulkData(template, data, columnDelimiter, Const.LF, nullValue);
    }

    public static InputStream buildBulkData(String template, List<Map<String, Object>> data,
                                            String columnDelimiter, String rowDelimiter, String nullValue) {
        return buildBulkData(template, data, columnDelimiter, rowDelimiter, nullValue, false);
    }

    public static InputStream buildBulkData(String template, List<Map<String, Object>> data,
                                            String columnDelimiter, String rowDelimiter, String nullValue,
                                            boolean withColumnNames) {
        if (StrKit.isBlank(columnDelimiter)) {
            columnDelimiter = Const.VERTICAL;
        }
        Assert.notNull(template, "参数 'template' 不能为空");
        StringBuilder sb = new StringBuilder();
        List<String> cols = Arrays.stream(template.split(Const.COMMA)).peek(StringUtils::trimAllWhitespace).toList();
        if (withColumnNames) {
            sb.append(String.join(columnDelimiter, cols)).append(rowDelimiter);
        }
        for (Map<String, Object> m : data) {
            var row = cols.stream().map(col -> {
                var v = m.get(col);
                if (Objects.isNull(v) || StrKit.isBlank(Objects.toString(v))) {
                    v = nullValue;
                }
                return Objects.toString(v);
            }).collect(Collectors.joining(columnDelimiter));
            sb.append(row).append(rowDelimiter);
        }
        if (log.isDebugEnabled()) {
            log.debug("Dataset ===> {}", sb);
        }
        return new ByteArrayInputStream(sb.toString().getBytes());
    }
}
