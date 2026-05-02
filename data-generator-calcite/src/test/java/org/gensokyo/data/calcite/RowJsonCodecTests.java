package org.gensokyo.data.calcite;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

class RowJsonCodecTests {

    @Test
    void encodesPrimitiveValuesAndNulls() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "alpha");
        values.put("score", 10);
        values.put("enabled", true);
        values.put("missing", null);

        String json = RowJsonCodec.toJsonObject(values);

        Assertions.assertEquals("{\"name\":\"alpha\",\"score\":10,\"enabled\":true,\"missing\":null}", json);
    }

    @Test
    void escapesKeysAndStringValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("a\"b", "x\\y\"z");

        String json = RowJsonCodec.toJsonObject(values);

        Assertions.assertEquals("{\"a\\\"b\":\"x\\\\y\\\"z\"}", json);
    }
}
