package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.codec.*;
import org.gensokyo.data.calcite.parser.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;
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

class CsvRowSinkAdapterTests {

    @Test
    void rejectsMultiCharacterDelimiter() throws Exception {
        Path csv = Files.createTempFile("template-v2-csv-sink", ".csv");
        WriterVO writer = writer(csv, Map.of("delimiter", "||"));

        IllegalArgumentException failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new CsvRowSinkAdapter(writer).write(schema(), List.of(row())));

        Assertions.assertTrue(failure.getMessage().contains("CSV sink delimiter must be exactly one character"));
        Assertions.assertTrue(failure.getMessage().contains(csv.toString()));
    }

    @Test
    void escapesConfiguredSingleCharacterDelimiter() throws Exception {
        Path csv = Files.createTempFile("template-v2-csv-sink-pipe", ".csv");
        WriterVO writer = writer(csv, Map.of("delimiter", "|"));

        new CsvRowSinkAdapter(writer).write(schema(), List.of(new Row(Map.of("name", "alpha|beta"))));

        Assertions.assertEquals(List.of("name", "\"alpha|beta\""), Files.readAllLines(csv));
    }

    private WriterVO writer(Path path, Map<String, Object> options) {
        WriterVO writer = new WriterVO();
        writer.setType("CSV");
        writer.setTarget(path.toString());
        writer.setOptions(options);
        return writer;
    }

    private RowSchema schema() {
        RowSchema schema = new RowSchema();
        schema.setColumns(List.of(new ColumnDef("name", "VARCHAR", true)));
        return schema;
    }

    private Row row() {
        return new Row(Map.of("name", "alpha"));
    }
}
