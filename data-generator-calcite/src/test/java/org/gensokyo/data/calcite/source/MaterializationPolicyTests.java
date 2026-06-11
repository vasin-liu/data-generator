/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.calcite.runtime.TemplateV2RuntimeRegistry;
import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.calcite.sink.ConsoleSinkFactory;
import org.gensokyo.data.calcite.sql.SqlTransformFactory;
import org.gensokyo.data.iterator.ConstantIteratorVO;
import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.MaterializationPolicyVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tests for {@link MaterializationPolicyVO} runtime behavior.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
class MaterializationPolicyTests {

    @Test
    void weightedModeProducesDeterministicDistributionOnFixedSeed() {
        MaterializationPolicyVO policy = weightedPolicy(List.of(1, 2, 1), 0L, null);

        List<String> firstRun = materializedValues(constantSource(List.of("a", "b", "c")), policy);
        List<String> secondRun = materializedValues(constantSource(List.of("a", "b", "c")), policy);

        Assertions.assertEquals(firstRun, secondRun);
        Assertions.assertEquals(4, firstRun.size());
        Assertions.assertEquals(1, count(firstRun, "a"));
        Assertions.assertEquals(2, count(firstRun, "b"));
        Assertions.assertEquals(1, count(firstRun, "c"));
    }

    @Test
    void orderedModePreservesSourceOrderAndLimit() {
        MaterializationPolicyVO policy = modePolicy("ORDERED", 2);

        List<String> values = materializedValues(numberSource(1, 5, 1), policy);

        Assertions.assertEquals(List.of("1", "2"), values);
    }

    @Test
    void limitModeRequiresPositiveLimit() {
        MaterializationPolicyVO policy = modePolicy("LIMIT", 3);

        List<String> values = materializedValues(numberSource(1, 5, 1), policy);

        Assertions.assertEquals(List.of("1", "2", "3"), values);
    }

    @Test
    void onceModeDeduplicatesRowsInSourceOrder() {
        MaterializationPolicyVO policy = modePolicy("ONCE", null);
        IteratorSourceVO source = constantSource(List.of("a", "b", "a", "c"));

        List<String> values = materializedValues(source, policy);

        Assertions.assertEquals(List.of("a", "b", "c"), values);
    }

    @Test
    void equalModeShufflesDeterministicallyWithSeed() {
        MaterializationPolicyVO policy = modePolicy("EQUAL", 3);
        policy.setSeed(7L);

        List<String> firstRun = materializedValues(numberSource(1, 5, 1), policy);
        List<String> secondRun = materializedValues(numberSource(1, 5, 1), policy);

        Assertions.assertEquals(firstRun, secondRun);
        Assertions.assertEquals(3, firstRun.size());
        Assertions.assertNotEquals(List.of("1", "2", "3"), firstRun);
    }

    @Test
    void runnerAppliesMaterializationPolicyBeforeSqlTransform() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("materialization-policy-v2");
        template.setSources(Map.of("seed", withPolicy(numberSource(1, 5, 1), modePolicy("ORDERED", 2))));
        template.setTransformers(List.of(sql("SELECT value FROM seed")));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(registry()).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        Assertions.assertEquals("1", result.getRows().getFirst().getString("value"));
        Assertions.assertEquals("2", result.getRows().get(1).getString("value"));
    }

    @Test
    void rejectsUnknownModeAtRuntime() {
        MaterializationPolicyVO policy = modePolicy("UNKNOWN", 1);

        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () ->
                registry().createSource("seed", withPolicy(numberSource(1, 3, 1), policy)));

        Assertions.assertTrue(failure.getCause().getMessage().contains("Unsupported materialization policy mode"));
    }

    @Test
    void rejectsNegativeWeightsAtRuntime() {
        MaterializationPolicyVO policy = weightedPolicy(List.of(1, -1), 0L, null);

        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () ->
                registry().createSource("seed", withPolicy(constantSource(List.of("a", "b")), policy)));

        Assertions.assertTrue(failure.getCause().getMessage().contains("weight"));
    }

    private static long count(List<String> values, String target) {
        return values.stream().filter(target::equals).count();
    }

    private List<String> materializedValues(IteratorSourceVO source, MaterializationPolicyVO policy) {
        RowSource rowSource = new MaterializationPolicyRowSource(new IteratorRowSource("seed", source), policy);
        return rowSource.rows().stream().map(row -> row.getString("value")).toList();
    }

    private IteratorSourceVO withPolicy(IteratorSourceVO source, MaterializationPolicyVO policy) {
        source.setMaterializationPolicy(policy);
        return source;
    }

    private MaterializationPolicyVO modePolicy(String mode, Integer limit) {
        MaterializationPolicyVO policy = new MaterializationPolicyVO();
        policy.setMode(mode);
        policy.setLimit(limit);
        return policy;
    }

    private MaterializationPolicyVO weightedPolicy(List<Integer> weights, long seed, Integer limit) {
        MaterializationPolicyVO policy = new MaterializationPolicyVO();
        policy.setMode("WEIGHTED");
        policy.setWeights(weights);
        policy.setSeed(seed);
        policy.setLimit(limit);
        return policy;
    }

    private IteratorSourceVO constantSource(List<String> dataset) {
        ConstantIteratorVO iterator = new ConstantIteratorVO();
        iterator.setType("constant");
        iterator.setDataset(dataset.stream().map(value -> (Object) value).collect(Collectors.toList()));
        iterator.setRepeat(1);
        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(iterator);
        return source;
    }

    private IteratorSourceVO numberSource(long from, long to, int step) {
        NumberIteratorVO iterator = new NumberIteratorVO();
        iterator.setType("number");
        iterator.setFrom(from);
        iterator.setTo(to);
        iterator.setStep(step);
        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(iterator);
        return source;
    }

    private SqlTransformVO sql(String sql) {
        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql(sql);
        return transform;
    }

    private WriteStageVO consoleSink() {
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(new ConsoleWriterVO()));
        return sink;
    }

    private TemplateV2RuntimeRegistry registry() {
        return new TemplateV2RuntimeRegistry(
                List.of(new IteratorSourceFactory()),
                List.of(new SqlTransformFactory()),
                List.of(new ConsoleSinkFactory())
        );
    }
}
