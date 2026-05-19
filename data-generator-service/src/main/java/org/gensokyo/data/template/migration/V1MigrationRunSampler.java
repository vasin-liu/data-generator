/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.gensokyo.data.calcite.RuntimeJdbcEndpointResolver;
import org.gensokyo.data.calcite.source.QueryRowSource;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.iterator.DatabaseIteratorVO;
import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.model.vo.iterator.IteratorVO;
import org.gensokyo.data.pipeline.DefaultDataPipelineTaskFactory;
import org.gensokyo.kit.collect.CollectKit;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Samples V1 template output for migration compare (JDBC iterator and number iterator paths).
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
final class V1MigrationRunSampler {

    private final DefaultDataPipelineTaskFactory v1TaskFactory;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RuntimeJdbcEndpointResolver jdbcEndpointResolver;

    V1MigrationRunSampler(
            DefaultDataPipelineTaskFactory v1TaskFactory,
            NamedParameterJdbcTemplate jdbcTemplate,
            RuntimeJdbcEndpointResolver jdbcEndpointResolver) {
        this.v1TaskFactory = Objects.requireNonNull(v1TaskFactory, "v1TaskFactory");
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.jdbcEndpointResolver = Objects.requireNonNull(jdbcEndpointResolver, "jdbcEndpointResolver");
    }

    /**
     * Attempts a bounded sample for the V1 template.
     *
     * @param v1          V1 template
     * @param sampleLimit maximum sample rows
     * @return outcome when a supported iterator shape is found
     */
    Optional<RunOutcome> trySample(TemplateVO v1, int sampleLimit) {
        DatabaseIteratorVO databaseIterator = findDatabaseIterator(v1.getIterator());
        if (databaseIterator != null) {
            return Optional.of(sampleDatabaseIterator(databaseIterator, sampleLimit));
        }
        NumberIteratorVO numberIterator = findNumberIterator(v1.getIterator());
        if (numberIterator != null) {
            return Optional.of(sampleNumberIterator(numberIterator, sampleLimit));
        }
        return Optional.empty();
    }

    /**
     * Runs the full V1 pipeline when iterator sampling is not available.
     *
     * @param v1 V1 template
     * @return outcome with zero rows when pipeline metrics are unavailable
     */
    RunOutcome runPipelineFallback(TemplateVO v1) {
        try {
            v1TaskFactory.newInstance(v1).call();
        }
        catch (Exception e) {
            throw new DataGeneratorException("V1 migration compare run failed", e);
        }
        return new RunOutcome(0, List.of());
    }

    private RunOutcome sampleDatabaseIterator(DatabaseIteratorVO iterator, int sampleLimit) {
        QuerySourceVO source = toQuerySource(iterator);
        String dataSourceId = jdbcEndpointResolver.resolveSourceDataSourceId(source);
        if (dataSourceId == null || dataSourceId.isBlank()) {
            throw new IllegalArgumentException("Database iterator requires a resolvable dataSourceId");
        }
        source.setDataSourceId(dataSourceId);

        try {
            DynamicDataSourceContextHolder.push(dataSourceId);
            QueryRowSource rowSource = new QueryRowSource("iterator", source, jdbcTemplate);
            List<Row> rows = rowSource.rows();
            long maxRows = iterator.getMaxRows() > 0 ? iterator.getMaxRows() : rows.size();
            long rowCount = Math.min(rows.size(), maxRows);
            List<Map<String, Object>> sample = new ArrayList<>();
            int limit = Math.min(sampleLimit, (int) rowCount);
            for (int i = 0; i < limit && i < rows.size(); i++) {
                sample.add(new LinkedHashMap<>(rows.get(i).values()));
            }
            return new RunOutcome(rowCount, sample);
        }
        finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

    private static RunOutcome sampleNumberIterator(NumberIteratorVO iterator, int sampleLimit) {
        long from = iterator.getFrom();
        long to = iterator.getTo();
        int step = iterator.getStep() > 0 ? iterator.getStep() : 1;
        if (to < from) {
            return new RunOutcome(0, List.of());
        }
        long count = ((to - from) / step) + 1;
        List<Map<String, Object>> sample = new ArrayList<>();
        int limit = (int) Math.min(sampleLimit, count);
        long value = from;
        for (int i = 0; i < limit; i++) {
            sample.add(Map.of("value", value));
            value += step;
        }
        return new RunOutcome(count, sample);
    }

    private static QuerySourceVO toQuerySource(DatabaseIteratorVO iterator) {
        QuerySourceVO source = new QuerySourceVO();
        source.setDataSourceId(iterator.getDataSourceId());
        source.setSql(iterator.getSql());
        source.setPageIndex(iterator.getPageIndex());
        source.setPageSize(iterator.getPageSize());
        source.setMaxRows(iterator.getMaxRows());
        if (CollectKit.isNotEmpty(iterator.getParams())) {
            source.setParams(iterator.getParams());
        }
        return source;
    }

    private static DatabaseIteratorVO findDatabaseIterator(IteratorVO iterator) {
        IteratorVO current = iterator;
        while (current != null) {
            if (current instanceof DatabaseIteratorVO database) {
                return database;
            }
            current = current.getIterator();
        }
        return null;
    }

    private static NumberIteratorVO findNumberIterator(IteratorVO iterator) {
        IteratorVO current = iterator;
        while (current != null) {
            if (current instanceof NumberIteratorVO number) {
                return number;
            }
            current = current.getIterator();
        }
        return null;
    }
}
