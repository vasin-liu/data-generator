package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.ColumnDef;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

class JsonRowSinkAdapterTests {

    @Test
    void writesJsonArrayByDefault() throws Exception {
        Path json = Files.createTempFile("template-v2-json-array-sink", ".json");

        new JsonRowSinkAdapter(writer(json, Map.of())).write(schema(), rows());

        String content = Files.readString(json);
        Assertions.assertTrue(content.startsWith("["));
        Assertions.assertTrue(content.endsWith("]"));
        Assertions.assertTrue(content.contains("\"name\":\"alpha\""));
        Assertions.assertTrue(content.contains("\"score\":10"));
    }

    @Test
    void writesNdjsonWhenConfigured() throws Exception {
        Path json = Files.createTempFile("template-v2-json-ndjson-sink", ".json");

        new JsonRowSinkAdapter(writer(json, Map.of("mode", "ndjson"))).write(schema(), rows());

        List<String> lines = Files.readAllLines(json);
        Assertions.assertEquals(2, lines.size());
        Assertions.assertEquals("{\"name\":\"alpha\",\"score\":10}", lines.get(0));
        Assertions.assertEquals("{\"name\":\"beta\",\"score\":20}", lines.get(1));
    }

    @Test
    void rejectsUnsupportedModeWithTargetDiagnostics() throws Exception {
        Path json = Files.createTempFile("template-v2-json-bad-mode-sink", ".json");
        WriterVO writer = writer(json, Map.of("mode", "object"));

        IllegalArgumentException failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new JsonRowSinkAdapter(writer).write(schema(), rows()));

        Assertions.assertTrue(failure.getMessage().contains("Unsupported JSON sink mode [OBJECT]"));
        Assertions.assertTrue(failure.getMessage().contains(json.toString()));
    }

    private WriterVO writer(Path path, Map<String, Object> options) {
        WriterVO writer = new WriterVO();
        writer.setType("JSON");
        writer.setTarget(path.toString());
        writer.setOptions(options);
        return writer;
    }

    private RowSchema schema() {
        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(
                new ColumnDef("name", "VARCHAR", true),
                new ColumnDef("score", "BIGINT", true)
        ));
        return schema;
    }

    private List<Row> rows() {
        return List.of(
                new Row(Map.of("name", "alpha", "score", 10)),
                new Row(Map.of("name", "beta", "score", 20))
        );
    }
}
