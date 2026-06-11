package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;
import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.SourcePolicyVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class SourcePolicyRowSourceTests {

    @Test
    void appliesOrderedLimitToSourceRows() {
        RowSource source = new SourcePolicyRowSource(new IteratorRowSource("seed", numberSource(1, 5, 1)),
                policy("REPEAT_ORDER", 2));

        Assertions.assertEquals(2, source.rows().size());
        Assertions.assertEquals("1", source.rows().getFirst().getString("value"));
        Assertions.assertEquals("2", source.rows().get(1).getString("value"));
    }

    @Test
    void appliesRandomLimitDeterministically() {
        RowSource firstRun = new SourcePolicyRowSource(new IteratorRowSource("seed", numberSource(1, 5, 1)),
                policy("REPEAT_RANDOM", 3));
        RowSource secondRun = new SourcePolicyRowSource(new IteratorRowSource("seed", numberSource(1, 5, 1)),
                policy("REPEAT_RANDOM", 3));

        Assertions.assertEquals(firstRun.rows(), secondRun.rows());
        Assertions.assertEquals(3, firstRun.rows().size());
    }

    @Test
    void treatsOrderAliasesAsOrderedMaterializationPlusLimit() {
        List<String> expected = List.of("1", "2");

        Assertions.assertEquals(expected, values(policy("ORDER", 2)));
        Assertions.assertEquals(expected, values(policy("FIRST", 2)));
        Assertions.assertEquals(expected, values(policy("REPEAT_ORDER", 2)));
        Assertions.assertEquals(expected, values(policy("ONCE_ORDER", 2)));
        Assertions.assertEquals(expected, values(policy("MULTIPLE_ORDER", 2)));
    }

    @Test
    void treatsRandomAliasesAsDeterministicShuffledMaterializationPlusLimit() {
        List<String> repeatRandom = values(policy("REPEAT_RANDOM", 3));

        Assertions.assertEquals(repeatRandom, values(policy("RANDOM", 3)));
        Assertions.assertEquals(repeatRandom, values(policy("ONCE_RANDOM", 3)));
    }

    @Test
    void defaultsBlankSelectionStrategyToOrder() {
        Assertions.assertEquals(List.of("1", "2"), values(policy("   ", 2)));
    }

    @Test
    void supportsNullSelectionStrategyAsOrder() {
        Assertions.assertEquals(List.of("1", "2"), values(policy(null, 2)));
    }

    @Test
    void currentPolicyDoesNotModelV1ConsumptiveSelectionSemantics() {
        List<String> firstRun = values(policy("ONCE_ORDER", 2));
        List<String> secondRun = values(policy("ONCE_ORDER", 2));

        Assertions.assertEquals(List.of("1", "2"), firstRun);
        Assertions.assertEquals(firstRun, secondRun);
    }

    @Test
    void rejectsNegativeLimit() {
        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () ->
                registry().createSource("seed", withPolicy(numberSource(1, 5, 1), policy("ORDER", -1))));

        Assertions.assertTrue(failure.getCause().getMessage().contains("limit"));
    }

    @Test
    void rejectsUnsupportedSelectionStrategy() {
        IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () ->
                registry().createSource("seed", withPolicy(numberSource(1, 5, 1), policy("UNKNOWN", 1))));

        Assertions.assertTrue(failure.getCause().getMessage().contains("Unsupported source selection strategy"));
    }

    @Test
    void runnerAppliesSourcePolicyBeforeSqlTransform() {
        TemplateV2VO template = new TemplateV2VO();
        template.setName("source-policy-v2");
        template.setSources(Map.of("seed", withPolicy(numberSource(1, 5, 1), policy("FIRST", 2))));
        template.setTransformers(List.of(sql("SELECT value FROM seed")));
        template.setSinks(List.of(consoleSink()));

        TemplateV2RunResult result = new TemplateV2Runner(registry()).run(template);

        Assertions.assertEquals(2, result.getRows().size());
        Assertions.assertEquals("1", result.getRows().getFirst().getString("value"));
        Assertions.assertEquals("2", result.getRows().get(1).getString("value"));
    }

    private IteratorSourceVO withPolicy(IteratorSourceVO source, SourcePolicyVO policy) {
        source.setPolicy(policy);
        return source;
    }

    private SourcePolicyVO policy(String selectionStrategy, Integer limit) {
        SourcePolicyVO policy = new SourcePolicyVO();
        policy.setSelectionStrategy(selectionStrategy);
        policy.setLimit(limit);
        return policy;
    }

    private List<String> values(SourcePolicyVO policy) {
        RowSource source = new SourcePolicyRowSource(new IteratorRowSource("seed", numberSource(1, 5, 1)), policy);
        return source.rows().stream().map(row -> row.getString("value")).toList();
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
