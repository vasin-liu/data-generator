/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.po.WriteStagePO;
import org.gensokyo.kit.character.StrKit;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public static List<Object> toList(Object dataset) {
        var results = Lists.newArrayList();
        if (Objects.isNull(dataset)) {
            return results;
        }
        if (dataset instanceof Collection<?> coll) {
            results.addAll(coll);
        } else if (dataset.getClass().isArray()) {
            results.addAll(Lists.newArrayList(dataset));
        } else if (dataset instanceof Map<?, ?> map) {
            results.addAll(map.values().stream()
                    .filter(Objects::nonNull)
                    .findFirst().stream().toList());
        } else {
            results.add(dataset);
        }
        return results;
    }

    public static InputStream buildBulkData(WriteStagePO wpo, List<Map<String, Object>> data) {
        return buildBulkData(wpo, data, Const.VERTICAL, Const.LF, Const.NULL);
    }

    public static InputStream buildBulkData(WriteStagePO wpo, List<Map<String, Object>> data, String columnDelimiter) {
        return buildBulkData(wpo, data, columnDelimiter, Const.LF, Const.NULL);
    }

    public static InputStream buildBulkData(WriteStagePO wpo, List<Map<String, Object>> data, String columnDelimiter,
                                            String nullValue) {
        return buildBulkData(wpo, data, columnDelimiter, Const.LF, nullValue);
    }

    public static InputStream buildBulkData(WriteStagePO wpo, List<Map<String, Object>> data,
                                            String columnDelimiter, String rowDelimiter, String nullValue) {
        return buildBulkData(wpo, data, columnDelimiter, rowDelimiter, nullValue, false);
    }

    public static InputStream buildBulkData(WriteStagePO wpo, List<Map<String, Object>> data,
                                            String columnDelimiter, String rowDelimiter, String nullValue,
                                            boolean withColumnNames) {
        if (StrKit.isBlank(columnDelimiter)) {
            columnDelimiter = Const.VERTICAL;
        }
        StringBuilder sb = new StringBuilder();
        List<String> cols = Splitter.on(Const.COMMA).trimResults().splitToList(wpo.getTemplate());
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
        log.debug("Dataset ===> {}", sb);
        return new ByteArrayInputStream(sb.toString().getBytes());
    }
}
