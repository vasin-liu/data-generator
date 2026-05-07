package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.codec.*;
import org.gensokyo.data.calcite.parser.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;
import org.gensokyo.data.iterator.ConstantIteratorVO;
import org.gensokyo.data.iterator.DateTimeIteratorVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

class IteratorRowSourceTests {

    @Test
    void materializesConstantIteratorRepeat() {
        ConstantIteratorVO iterator = new ConstantIteratorVO();
        iterator.setDataset(List.of("alpha", "beta"));
        iterator.setRepeat(2);

        RowSource source = new IteratorRowSource("constant_seed", source(iterator));

        Assertions.assertEquals(4, source.rows().size());
        Assertions.assertEquals("alpha", source.rows().get(0).getString("value"));
        Assertions.assertEquals("beta", source.rows().get(1).getString("value"));
        Assertions.assertEquals("alpha", source.rows().get(2).getString("value"));
        Assertions.assertEquals("beta", source.rows().get(3).getString("value"));
        Assertions.assertEquals("VARCHAR", source.schema().getColumns().getFirst().getLogicalType());
    }

    @Test
    void rejectsInfiniteConstantIteratorRepeat() {
        ConstantIteratorVO iterator = new ConstantIteratorVO();
        iterator.setDataset(List.of("alpha"));
        iterator.setRepeat(-1);

        IllegalArgumentException failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new IteratorRowSource("constant_seed", source(iterator)));

        Assertions.assertTrue(failure.getMessage().contains("repeat [-1]"));
        Assertions.assertTrue(failure.getMessage().contains("finite Template V2 materialization"));
    }

    @Test
    void materializesDatetimeIterator() {
        DateTimeIteratorVO iterator = new DateTimeIteratorVO();
        iterator.setFrom(LocalDateTime.parse("2026-05-01T00:00:00"));
        iterator.setTo(LocalDateTime.parse("2026-05-03T00:00:00"));
        iterator.setStep(1);
        iterator.setUnit(ChronoUnit.DAYS);

        RowSource source = new IteratorRowSource("datetime_seed", source(iterator));

        Assertions.assertEquals(3, source.rows().size());
        Assertions.assertEquals("2026-05-01T00:00", source.rows().get(0).getString("value"));
        Assertions.assertEquals("2026-05-02T00:00", source.rows().get(1).getString("value"));
        Assertions.assertEquals("2026-05-03T00:00", source.rows().get(2).getString("value"));
        Assertions.assertEquals("TIMESTAMP", source.schema().getColumns().getFirst().getLogicalType());
    }

    @Test
    void rejectsInvalidDatetimeRange() {
        DateTimeIteratorVO iterator = new DateTimeIteratorVO();
        iterator.setFrom(LocalDateTime.parse("2026-05-03T00:00:00"));
        iterator.setTo(LocalDateTime.parse("2026-05-01T00:00:00"));

        IllegalArgumentException failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new IteratorRowSource("datetime_seed", source(iterator)));

        Assertions.assertTrue(failure.getMessage().contains("from must not be after to"));
    }

    private IteratorSourceVO source(org.gensokyo.data.model.vo.iterator.IteratorVO iterator) {
        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(iterator);
        return source;
    }
}
