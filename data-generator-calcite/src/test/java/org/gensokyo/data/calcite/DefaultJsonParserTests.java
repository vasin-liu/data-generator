package org.gensokyo.data.calcite;

import org.gensokyo.data.calcite.codec.*;
import org.gensokyo.data.calcite.parser.*;
import org.gensokyo.data.calcite.plugin.*;
import org.gensokyo.data.calcite.runtime.*;
import org.gensokyo.data.calcite.sink.*;
import org.gensokyo.data.calcite.source.*;
import org.gensokyo.data.calcite.sql.*;
import org.gensokyo.data.model.v2.JsonSourceVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class DefaultJsonParserTests {

    @Test
    void rejectsMissingRootSelectorSegment() {
        JsonSourceVO source = source("missing-root.json", "payload.people");

        IllegalArgumentException failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new DefaultJsonParser().parse(source, "{\"payload\":{}}"));

        Assertions.assertTrue(failure.getMessage().contains("root selector [payload.people]"));
        Assertions.assertTrue(failure.getMessage().contains("segment [people]"));
        Assertions.assertTrue(failure.getMessage().contains("missing-root.json"));
    }

    @Test
    void rejectsOutOfRangeRootSelectorArrayIndex() {
        JsonSourceVO source = source("missing-index.json", "payload.people[1]");

        IllegalArgumentException failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new DefaultJsonParser().parse(source, "{\"payload\":{\"people\":[{\"name\":\"alpha\"}]}}"));

        Assertions.assertTrue(failure.getMessage().contains("root selector [payload.people[1]]"));
        Assertions.assertTrue(failure.getMessage().contains("segment [people[1]]"));
        Assertions.assertTrue(failure.getMessage().contains("missing-index.json"));
    }

    @Test
    void rejectsInvalidRootSelectorArrayIndex() {
        JsonSourceVO source = source("bad-index.json", "payload.people[x]");

        IllegalArgumentException failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> new DefaultJsonParser().parse(source, "{\"payload\":{\"people\":[]}}"));

        Assertions.assertTrue(failure.getMessage().contains("Invalid array index"));
        Assertions.assertTrue(failure.getMessage().contains("payload.people[x]"));
    }

    @Test
    void returnsNoRowsWhenRootSelectorMatchesJsonNull() {
        JsonSourceVO source = source("null-root.json", "payload.people");

        List<java.util.Map<String, Object>> rows = new DefaultJsonParser().parse(source,
                "{\"payload\":{\"people\":null}}");

        Assertions.assertTrue(rows.isEmpty());
    }

    private JsonSourceVO source(String path, String root) {
        JsonSourceVO source = new JsonSourceVO();
        source.setPath(path);
        source.setRoot(root);
        return source;
    }
}
