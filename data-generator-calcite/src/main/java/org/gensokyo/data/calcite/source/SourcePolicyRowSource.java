package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.parser.*;

import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SourcePolicyVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class SourcePolicyRowSource implements RowSource {
    private static final long DETERMINISTIC_RANDOM_SEED = 0L;

    private final RowSource delegate;
    private final List<Row> rows;

    public SourcePolicyRowSource(RowSource delegate, SourcePolicyVO policy) {
        this.delegate = delegate;
        this.rows = applyPolicy(delegate.rows(), policy);
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public RowSchema schema() {
        return delegate.schema();
    }

    @Override
    public List<Row> rows() {
        return rows;
    }

    private List<Row> applyPolicy(List<Row> sourceRows, SourcePolicyVO policy) {
        if (policy == null) {
            return sourceRows;
        }
        List<Row> selected = switch (selectionStrategy(policy)) {
            case "REPEAT_RANDOM", "ONCE_RANDOM", "RANDOM" -> randomRows(sourceRows);
            case "FIRST", "REPEAT_ORDER", "ONCE_ORDER", "MULTIPLE_ORDER", "ORDER" -> orderedRows(sourceRows);
            default -> throw new IllegalArgumentException("Unsupported source selection strategy: "
                    + policy.getSelectionStrategy());
        };
        return limit(selected, policy.getLimit());
    }

    private String selectionStrategy(SourcePolicyVO policy) {
        if (policy.getSelectionStrategy() == null || policy.getSelectionStrategy().isBlank()) {
            return "ORDER";
        }
        return policy.getSelectionStrategy().trim().toUpperCase(Locale.ROOT);
    }

    private List<Row> orderedRows(List<Row> sourceRows) {
        return List.copyOf(sourceRows);
    }

    private List<Row> randomRows(List<Row> sourceRows) {
        List<Row> rows = new ArrayList<>(sourceRows);
        Collections.shuffle(rows, new Random(DETERMINISTIC_RANDOM_SEED));
        return List.copyOf(rows);
    }

    private List<Row> limit(List<Row> sourceRows, Integer limit) {
        if (limit == null) {
            return sourceRows;
        }
        if (limit < 0) {
            throw new IllegalArgumentException("Source policy limit must be greater than or equal to 0");
        }
        if (limit >= sourceRows.size()) {
            return sourceRows;
        }
        return List.copyOf(sourceRows.subList(0, limit));
    }
}
