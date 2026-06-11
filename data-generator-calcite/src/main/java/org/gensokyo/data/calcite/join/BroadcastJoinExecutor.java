/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.join;

import org.apache.calcite.sql.JoinType;
import org.gensokyo.data.calcite.sql.CalciteRowTransformer;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Hash-joins a fact chunk against a materialized broadcast dimension snapshot.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public final class BroadcastJoinExecutor {

    private BroadcastJoinExecutor() {
    }

    /**
     * Joins fact rows to the broadcast snapshot and projects output columns from the transform SELECT list.
     *
     * @param factChunk fact rows for one chunk (lowercase column keys)
     * @param snapshot  materialized dimension index
     * @param spec      parsed join specification
     * @return joined rows and output schema matching the SQL projection
     */
    public static CalciteRowTransformer.TransformResult join(
            List<Row> factChunk,
            BroadcastJoinSnapshot snapshot,
            BroadcastJoinSpec spec) {
        List<Row> joined = new ArrayList<>(factChunk.size());
        String factJoinColumn = spec.factJoinColumn().toLowerCase(Locale.ROOT);
        boolean leftJoin = spec.joinType() == JoinType.LEFT;

        for (Row factRow : factChunk) {
            Object joinKey = factRow.get(factJoinColumn);
            Row dimRow = snapshot.lookup(joinKey);
            if (dimRow == null && !leftJoin) {
                continue;
            }
            joined.add(projectRow(factRow, dimRow, spec));
        }

        RowSchema schema = spec.outputSchema();
        return new CalciteRowTransformer.TransformResult(schema, List.copyOf(joined));
    }

    private static Row projectRow(Row factRow, Row dimRow, BroadcastJoinSpec spec) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (BroadcastJoinSpec.OutputColumn column : spec.outputColumns()) {
            String sourceColumn = column.sourceColumn().toLowerCase(Locale.ROOT);
            Object value = switch (column.side()) {
                case FACT -> factRow.get(sourceColumn);
                case DIM -> dimRow == null ? null : dimRow.get(sourceColumn);
            };
            values.put(column.name().toLowerCase(Locale.ROOT), value);
        }
        return new Row(values);
    }
}
