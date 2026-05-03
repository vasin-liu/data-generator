package org.gensokyo.data.calcite;

import org.gensokyo.data.model.v2.JsonSourceVO;

import java.util.List;
import java.util.Map;

public interface JsonParser {
    List<Map<String, Object>> parse(JsonSourceVO source, String content);
}
