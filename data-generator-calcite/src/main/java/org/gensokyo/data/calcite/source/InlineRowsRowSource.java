/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.InlineRowsSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Row source backed by template-embedded static rows.
 *
 * @author Gensokyo
 * @since 2026-06-07
 */
public class InlineRowsRowSource implements RowSource {

    private final String name;
    private final RowSchema schema;
    private final List<Row> rows;

    /**
     * Materializes declared inline rows for Calcite SQL.
     *
     * @param name   logical source name
     * @param source inline rows configuration
     */
    public InlineRowsRowSource(String name, InlineRowsSourceVO source) {
        this.name = Objects.requireNonNull(name, "name");
        Objects.requireNonNull(source, "source");
        List<Map<String, Object>> rowMaps = source.rowMaps();
        if (rowMaps.isEmpty()) {
            throw new IllegalArgumentException("Inline rows source [" + name + "] must contain at least one row");
        }
        this.rows = rowMaps.stream()
                .map(values -> new Row(new LinkedHashMap<>(values)))
                .toList();
        this.schema = resolveSchema(source.getSchema(), rowMaps);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public RowSchema schema() {
        return schema;
    }

    @Override
    public List<Row> rows() {
        return rows;
    }

    private static RowSchema resolveSchema(RowSchema declared, List<Map<String, Object>> rowMaps) {
        if (declared != null && declared.getColumns() != null && !declared.getColumns().isEmpty()) {
            return declared;
        }
        Map<String, Object> first = rowMaps.getFirst();
        RowSchema inferred = new RowSchema();
        List<ColumnDef> columns = new ArrayList<>();
        for (String key : first.keySet()) {
            columns.add(new ColumnDef(key, "VARCHAR", true));
        }
        inferred.setColumns(columns);
        return inferred;
    }
}
