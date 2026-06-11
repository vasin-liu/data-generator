/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import org.gensokyo.data.calcite.source.IteratorSourceFactory;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.calcite.sink.ConsoleSinkFactory;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.workflow.ComputeBlockVO;
import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.calcite.RowSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tests in-process partitioned compute execution and run-metrics aggregation.
 *
 * @author Gensokyo
 * @since 2026-06-01
 */
class PartitionedComputeBlockRunnerTests {

    @Test
    void roundRobinPartitionsMergeAllRows() {
        ComputeBlockVO block = new ComputeBlockVO();
        block.setId("p-block");
        block.setSources(Map.of("seed", numberSource(1, 10, 1)));
        block.setTransformers(List.of(sql("SELECT value FROM seed")));

        ExecutionPolicyVO policyVO = new ExecutionPolicyVO();
        policyVO.setPartitionCount(4);
        policyVO.setMode("IN_MEMORY");
        EffectiveExecutionPolicy policy = EffectiveExecutionPolicy.resolve(policyVO);

        PartitionedComputeBlockRunner runner = new PartitionedComputeBlockRunner();
        TemplateV2RunResult result = runner.run(block, policy, defaultRegistry());

        Assertions.assertNotNull(result.getMetrics());
        Assertions.assertEquals(10, result.getRows().size());
        Assertions.assertEquals(Set.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
                result.getRows().stream().map(r -> r.getString("value")).collect(Collectors.toSet()));

        Assertions.assertEquals(4, result.getMetrics().getConfiguredPartitions());
        Assertions.assertEquals(4, result.getMetrics().getExecutedPartitions());
        Assertions.assertEquals(10, result.getMetrics().getTotalRowsRead());
    }

    @Test
    void hashPartitionsReportExecutedPartitionCount() {
        int partitionCount = 3;
        String partitionKey = "value";

        ExecutionPolicyVO policyVO = new ExecutionPolicyVO();
        policyVO.setPartitionCount(partitionCount);
        policyVO.setPartitionKey(partitionKey);
        policyVO.setMode("IN_MEMORY");
        EffectiveExecutionPolicy policy = EffectiveExecutionPolicy.resolve(policyVO);

        TemplateV2RuntimeRegistry registry = defaultRegistry();
        RowSource rowSource = registry.createSource("seed", numberSource(1, 10, 1), policy);
        List<Row> sourceRows = rowSource.rows();
        List<List<Row>> partitions = RowPartitioner.partition(sourceRows, partitionCount, partitionKey);
        int expectedExecuted = (int) partitions.stream().filter(bucket -> !bucket.isEmpty()).count();

        ComputeBlockVO block = new ComputeBlockVO();
        block.setId("p-block");
        block.setSources(Map.of("seed", numberSource(1, 10, 1)));
        block.setTransformers(List.of(sql("SELECT value FROM seed")));

        PartitionedComputeBlockRunner runner = new PartitionedComputeBlockRunner();
        TemplateV2RunResult result = runner.run(block, policy, registry);

        Assertions.assertEquals(10, result.getMetrics().getTotalRowsRead());
        Assertions.assertEquals(partitionCount, result.getMetrics().getConfiguredPartitions());
        Assertions.assertEquals(expectedExecuted, result.getMetrics().getExecutedPartitions());
    }

    private static IteratorSourceVO numberSource(long from, long to, int step) {
        NumberIteratorVO iterator = new NumberIteratorVO();
        iterator.setType("number");
        iterator.setFrom(from);
        iterator.setTo(to);
        iterator.setStep(step);
        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(iterator);
        return source;
    }

    private static SqlTransformVO sql(String sql) {
        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql(sql);
        return transform;
    }

    private static TemplateV2RuntimeRegistry defaultRegistry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new IteratorSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory()));
    }
}

