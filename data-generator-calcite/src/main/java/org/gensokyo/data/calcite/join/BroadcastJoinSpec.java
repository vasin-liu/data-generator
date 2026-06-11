/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.join;

import org.apache.calcite.sql.JoinType;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.List;

/**
 * Parsed broadcast-join plan: fact/dim source names, equi-join keys, join type, and SELECT projections.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public record BroadcastJoinSpec(
        JoinType joinType,
        String factSourceName,
        String dimSourceName,
        String factJoinColumn,
        String dimJoinColumn,
        List<OutputColumn> outputColumns,
        RowSchema outputSchema) {

    /**
     * One projected output column from the transform SELECT list.
     *
     * @param name       output column name (sink / result row key)
     * @param side       whether the value comes from the fact or dimension side
     * @param sourceColumn unqualified source column name on that side
     */
    public record OutputColumn(String name, ProjectionSide side, String sourceColumn) {
    }

    /**
     * Which side of the join supplies a projected column.
     */
    public enum ProjectionSide {
        FACT,
        DIM
    }
}
