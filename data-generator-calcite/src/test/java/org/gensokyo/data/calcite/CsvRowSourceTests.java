package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.CsvSourceVO;
import org.gensokyo.data.model.v2.Row;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class CsvRowSourceTests {

    @Test
    void rejectsRowWidthMismatchByDefault() throws Exception {
        Path csv = Files.createTempFile("template-v2-csv-width", ".csv");
        Files.writeString(csv, """
                name,score
                alpha,10,extra
                """);
        CsvSourceVO source = source(csv);

        IllegalArgumentException failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new CsvRowSource("people", source).rows());

        Assertions.assertTrue(failure.getMessage().contains("row width mismatch"));
        Assertions.assertTrue(failure.getMessage().contains("line [2]"));
        Assertions.assertTrue(failure.getMessage().contains(csv.toString()));
        Assertions.assertTrue(failure.getMessage().contains("expected [2]"));
        Assertions.assertTrue(failure.getMessage().contains("got [3]"));
    }

    @Test
    void allowsRowWidthMismatchWhenStrictColumnsIsDisabled() throws Exception {
        Path csv = Files.createTempFile("template-v2-csv-loose-width", ".csv");
        Files.writeString(csv, """
                name,score,city
                alpha,10
                """);
        CsvSourceVO source = source(csv);
        source.setStrictColumns(false);

        List<Row> rows = new CsvRowSource("people", source).rows();

        Assertions.assertEquals(1, rows.size());
        Assertions.assertEquals("alpha", rows.getFirst().getString("name"));
        Assertions.assertEquals("10", rows.getFirst().getString("score"));
        Assertions.assertNull(rows.getFirst().get("city"));
    }

    private CsvSourceVO source(Path path) {
        CsvSourceVO source = new CsvSourceVO();
        source.setPath(path.toString());
        return source;
    }
}
