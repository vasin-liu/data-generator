package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.parser.*;

import org.gensokyo.data.model.v2.JsonSourceVO;
import org.gensokyo.data.model.v2.SourceVO;

public class JsonSourceFactory implements V2SourceFactory {
    private final JsonParser jsonParser;

    public JsonSourceFactory() {
        this(new DefaultJsonParser());
    }

    public JsonSourceFactory(JsonParser jsonParser) {
        this.jsonParser = jsonParser;
    }

    @Override
    public boolean supports(SourceVO source) {
        return source instanceof JsonSourceVO;
    }

    @Override
    public RowSource create(String name, SourceVO source) {
        return new JsonRowSource(name, (JsonSourceVO) source, jsonParser);
    }
}
