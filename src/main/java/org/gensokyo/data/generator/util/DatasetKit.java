/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.gensokyo.data.generator.constant.Const;
import org.gensokyo.data.generator.domain.WriterPO;
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

    public static InputStream buildBulkData(WriterPO wpo, List<Map<String, Object>> data) {
        StringBuilder sb = new StringBuilder();
        List<String> cols = Splitter.on(Const.COMMA).trimResults().splitToList(wpo.getTemplate());
        for (Map<String, Object> m : data) {
            var row = cols.stream().map(col -> {
                var v = m.get(col);
                if (Objects.isNull(v) || StrKit.isBlank(Objects.toString(v))) {
                    v = Const.NULL;
                }
                return Objects.toString(v);
            }).collect(Collectors.joining(Const.VERTICAL));
            sb.append(row);
            sb.append("\n");
        }
        return new ByteArrayInputStream(sb.toString().getBytes());
    }
}
