package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.codec.*;
import org.gensokyo.data.calcite.parser.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;
import org.gensokyo.data.model.v2.CsvSourceVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class DefaultCsvParserTests {

    @Test
    void rejectsMultiCharacterDelimiter() {
        CsvSourceVO source = source("bad-delimiter.csv");
        source.setDelimiter("||");

        IllegalArgumentException failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new DefaultCsvParser().parse(source, List.of("a||b")));

        Assertions.assertTrue(failure.getMessage().contains("delimiter must be exactly one character"));
        Assertions.assertTrue(failure.getMessage().contains("bad-delimiter.csv"));
    }

    @Test
    void rejectsUnclosedQuotedFieldWithLineNumber() {
        CsvSourceVO source = source("bad-quote.csv");

        IllegalArgumentException failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new DefaultCsvParser().parse(source, List.of("name,city", "alpha,\"Paris")));

        Assertions.assertTrue(failure.getMessage().contains("Unclosed quoted CSV field"));
        Assertions.assertTrue(failure.getMessage().contains("line [2]"));
        Assertions.assertTrue(failure.getMessage().contains("bad-quote.csv"));
    }

    private CsvSourceVO source(String path) {
        CsvSourceVO source = new CsvSourceVO();
        source.setPath(path);
        return source;
    }
}
