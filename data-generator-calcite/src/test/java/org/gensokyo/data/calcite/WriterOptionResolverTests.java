package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.codec.*;
import org.gensokyo.data.calcite.parser.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class WriterOptionResolverTests {

    @Test
    void resolvesStringBooleanAndMapOptions() {
        WriterVO writer = new WriterVO();
        writer.setOptions(Map.of(
                "key", "${device}",
                "upsert", "true",
                "headers", Map.of("device", "${device}", "source", "v2")
        ));
        Row row = new Row(Map.of("device", "d1"));

        Assertions.assertEquals("d1", WriterOptionResolver.stringOption(writer, "key", row));
        Assertions.assertTrue(WriterOptionResolver.booleanOption(writer, "upsert"));
        Assertions.assertEquals(Map.of("device", "d1", "source", "v2"),
                WriterOptionResolver.stringMapOption(writer, "headers", row));
    }

    @Test
    void missingOptionsResolveToEmptyValues() {
        WriterVO writer = new WriterVO();
        Row row = new Row(Map.of("device", "d1"));

        Assertions.assertNull(WriterOptionResolver.stringOption(writer, "key", row));
        Assertions.assertFalse(WriterOptionResolver.booleanOption(writer, "upsert"));
        Assertions.assertTrue(WriterOptionResolver.stringMapOption(writer, "headers", row).isEmpty());
    }
}
