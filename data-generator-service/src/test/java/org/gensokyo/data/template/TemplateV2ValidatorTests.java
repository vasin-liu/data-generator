/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.model.v2.CsvSourceVO;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.JsTransformVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Publish-time upsert validation and IN_MEMORY large-file warnings for Template V2 (Phase 8, RW-03/RW-04).
 *
 * @author Gensokyo
 * @since 2026-06-29
 */
class TemplateV2ValidatorTests {

  @Test
  void upsertMissingKeysThrows() {
    TemplateV2VO template = baseTemplate(
        sql("SELECT id, label FROM input"),
        jdbcUpsertWriter(true, List.of()));

    IllegalArgumentException ex = Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> TemplateV2Validator.validate(template));

    Assertions.assertTrue(ex.getMessage().contains("sink[0].writer[0]"));
    Assertions.assertTrue(ex.getMessage().contains("upsertKeys"));
  }

  @Test
  void simpleSqlUpsertKeyMissingAtPublishThrows() {
    TemplateV2VO template = baseTemplate(
        sql("SELECT id, label FROM input"),
        jdbcUpsertWriter(true, List.of("missing_key")));

    IllegalArgumentException ex = Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> TemplateV2Validator.validate(template));

    Assertions.assertTrue(ex.getMessage().contains("missing_key"));
    Assertions.assertTrue(ex.getMessage().contains("transform output columns"));
  }

  @Test
  void opaqueTransformUpsertKeysRunOnlyWarning() {
    JsTransformVO js = new JsTransformVO();
    js.setType("js");
    js.setScript("function transform(rows) { return rows; }");

    TemplateV2VO template = baseTemplate(js, jdbcUpsertWriter(true, List.of("id")));

    Assertions.assertDoesNotThrow(() -> TemplateV2Validator.validate(template));

    List<String> warnings = TemplateV2Validator.collectWarnings(template);

    Assertions.assertTrue(warnings.stream().anyMatch(w -> w.contains("validated at run")));
  }

  @Test
  void largeFileInMemoryWarns(@TempDir Path tempDir) throws IOException {
    Path largeCsv = tempDir.resolve("large.csv");
    Files.write(largeCsv, new byte[(int) (10L * 1024L * 1024L)]);

    CsvSourceVO csv = new CsvSourceVO();
    csv.setPath(largeCsv.toString());
    csv.setHeader(true);

    TemplateV2VO template = baseTemplate(sql("SELECT c1 FROM incoming"), consoleSink());
    template.setSources(Map.of("incoming", csv));
    ExecutionPolicyVO policy = new ExecutionPolicyVO();
    policy.setMode("IN_MEMORY");
    template.setExecutionPolicy(policy);

    List<String> warnings = TemplateV2Validator.collectWarnings(template);

    Assertions.assertEquals(1, warnings.size());
    Assertions.assertTrue(warnings.getFirst().contains("CHUNKED") || warnings.getFirst().contains("STREAMING"));
    Assertions.assertTrue(warnings.getFirst().contains("10 MB"));
  }

  @Test
  void largeRowCountInMemoryWarns() {
    CsvSourceVO csv = new CsvSourceVO();
    csv.setPath("template/v2-scenarios/fixtures/orders.csv");
    csv.setHeader(true);
    csv.setMaxRows(100_000L);

    TemplateV2VO template = baseTemplate(sql("SELECT order_id FROM incoming"), consoleSink());
    template.setSources(Map.of("incoming", csv));
    ExecutionPolicyVO policy = new ExecutionPolicyVO();
    policy.setMode("IN_MEMORY");
    template.setExecutionPolicy(policy);

    List<String> warnings = TemplateV2Validator.collectWarnings(template);

    Assertions.assertEquals(1, warnings.size());
    Assertions.assertTrue(warnings.getFirst().contains("CHUNKED") || warnings.getFirst().contains("STREAMING"));
  }

  @Test
  void smallFileNoWarning() throws IOException {
    String yaml = """
        name: scenario-c-csv-export
        sources:
          incoming:
            type: csv
            path: template/v2-scenarios/fixtures/orders.csv
            header: true
        transform:
          type: sql
          sql: SELECT order_id, UPPER(customer) AS customer, amount FROM incoming
        sink:
          writers:
            - type: jdbc
              target: exported_orders
        """;
    TemplateV2DraftVO draft = new JacksonParser().parse(yaml, TemplateV2DraftVO.class);
    TemplateV2VO template = TemplateV2Normalizer.normalize(draft);
    ExecutionPolicyVO policy = new ExecutionPolicyVO();
    policy.setMode("IN_MEMORY");
    template.setExecutionPolicy(policy);

    Assertions.assertDoesNotThrow(() -> TemplateV2Validator.validate(template));

    List<String> warnings = TemplateV2Validator.collectWarnings(template);

    Assertions.assertTrue(warnings.isEmpty());
  }

  private static TemplateV2VO baseTemplate(org.gensokyo.data.model.v2.TransformVO transform, WriteStageVO sink) {
    TemplateV2VO template = new TemplateV2VO();
    template.setName("demo");
    template.setSources(Map.of("input", new IteratorSourceVO()));
    template.setTransformers(List.of(transform));
    template.setSinks(List.of(sink));
    return template;
  }

  private static SqlTransformVO sql(String content) {
    SqlTransformVO transform = new SqlTransformVO();
    transform.setType("sql");
    transform.setSql(content);
    return transform;
  }

  private static WriteStageVO jdbcUpsertWriter(boolean upsert, List<String> upsertKeys) {
    JdbcWriterVO writer = new JdbcWriterVO();
    writer.setTarget("orders_out");
    writer.getOptions().put("upsert", upsert);
    writer.getOptions().put("upsertKeys", upsertKeys);
    WriteStageVO sink = new WriteStageVO();
    sink.setWriters(List.of(writer));
    return sink;
  }

  private static WriteStageVO consoleSink() {
    WriteStageVO sink = new WriteStageVO();
    sink.setWriters(List.of(new org.gensokyo.data.model.vo.writer.ConsoleWriterVO()));
    return sink;
  }
}
