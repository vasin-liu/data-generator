/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.join;

import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.calcite.runtime.ScaleLimitExceededException;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
/**
 * In-memory hash index of a broadcast (dimension) side keyed by join column values.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public final class BroadcastJoinSnapshot {

    private final RowSchema dimSchema;
    private final Map<Object, Row> index;
    private final int rowCount;

    private BroadcastJoinSnapshot(RowSchema dimSchema, Map<Object, Row> index) {
        this.dimSchema = dimSchema;
        this.index = index;
        this.rowCount = index.size();
    }

    /**
     * Materializes dimension rows and builds a join-key index.
     *
     * @param dim              dimension row source (fully read)
     * @param dimJoinColumn    unqualified join column on the dimension side (lowercase)
     * @param broadcastMaxRows maximum allowed dimension rows
     * @param dimSourceName    template source name (for limit diagnostics)
     * @return snapshot for hash lookups
     */
    public static BroadcastJoinSnapshot materialize(
            RowSource dim,
            String dimJoinColumn,
            int broadcastMaxRows,
            String dimSourceName) {
        List<Row> rows = dim.rows();
        if (rows.size() > broadcastMaxRows) {
            throw new ScaleLimitExceededException(
                    "broadcastMaxRows",
                    broadcastMaxRows,
                    rows.size(),
                    "BROADCAST_MATERIALIZE",
                    dimSourceName);
        }
        String joinKey = dimJoinColumn.toLowerCase(Locale.ROOT);
        Map<Object, Row> index = new LinkedHashMap<>();
        for (Row row : rows) {
            Object key = normalizeKey(row.get(joinKey));
            // Last row wins for duplicate dimension keys (deterministic iteration order).
            index.put(key, row);
        }
        RowSchema schema = dim.schema();
        return new BroadcastJoinSnapshot(schema, Map.copyOf(index));
    }

    /**
     * Dimension schema from materialization.
     *
     * @return row schema
     */
    public RowSchema dimSchema() {
        return dimSchema;
    }

    /**
     * Number of indexed dimension rows (after duplicate-key collapse).
     *
     * @return index size
     */
    public int rowCount() {
        return rowCount;
    }

    /**
     * Looks up a dimension row by join key value.
     *
     * @param joinKey join key from a fact row (may be null)
     * @return matching dimension row, or {@code null} if none
     */
    public Row lookup(Object joinKey) {
        return index.get(normalizeKey(joinKey));
    }

    private static Object normalizeKey(Object key) {
        if (key instanceof Number number) {
            if (number instanceof Long || number instanceof Integer || number instanceof Short || number instanceof Byte) {
                return number.longValue();
            }
            if (number instanceof Double || number instanceof Float) {
                return number.doubleValue();
            }
        }
        return key;
    }
}
