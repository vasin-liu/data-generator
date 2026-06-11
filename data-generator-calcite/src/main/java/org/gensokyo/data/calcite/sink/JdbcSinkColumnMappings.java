/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.constant.Const;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves JDBC sink column mappings and row parameter maps.
 *
 * @author Gensokyo
 * @since 2026-06-11
 */
final class JdbcSinkColumnMappings {

    private JdbcSinkColumnMappings() {
    }

    /**
     * @param schema row schema from the pipeline
     * @param writer JDBC writer configuration
     * @return ordered source-to-target column mappings
     */
    static List<ColumnMapping> resolve(RowSchema schema, JdbcWriterVO writer) {
        if (schema == null || schema.getColumns() == null || schema.getColumns().isEmpty()) {
            throw new IllegalArgumentException("JDBC sink requires at least one output column");
        }
        if (!StringUtils.hasText(writer.getTemplate())) {
            return schema.getColumns().stream()
                    .map(column -> new ColumnMapping(column.getName(), column.getName()))
                    .toList();
        }
        List<ColumnMapping> mappings = new ArrayList<>();
        for (String token : writer.getTemplate().split(Const.COMMA)) {
            String item = token == null ? null : token.trim();
            if (!StringUtils.hasText(item)) {
                continue;
            }
            int split = item.indexOf(Const.COLON);
            if (split < 0) {
                mappings.add(new ColumnMapping(item, item));
            } else {
                String target = item.substring(0, split).trim();
                String source = item.substring(split + 1).trim();
                if (!StringUtils.hasText(target) || !StringUtils.hasText(source)) {
                    throw new IllegalArgumentException("Invalid JDBC sink template item: " + item);
                }
                mappings.add(new ColumnMapping(target, source));
            }
        }
        if (mappings.isEmpty()) {
            throw new IllegalArgumentException("JDBC sink template resolved to no columns");
        }
        return mappings;
    }

    /**
     * @param row      pipeline row
     * @param mappings column mappings
     * @return named SQL parameters keyed by target column
     */
    static Map<String, Object> toSqlParams(Row row, List<ColumnMapping> mappings) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (ColumnMapping mapping : mappings) {
            params.put(mapping.target(), row.get(mapping.source()));
        }
        return params;
    }

    /**
     * @param row      pipeline row
     * @param mappings column mappings
     * @return values ordered by target column list
     */
    static List<Object> orderedValues(Row row, List<ColumnMapping> mappings) {
        return mappings.stream().map(mapping -> row.get(mapping.source())).toList();
    }

    record ColumnMapping(String target, String source) {
    }
}
