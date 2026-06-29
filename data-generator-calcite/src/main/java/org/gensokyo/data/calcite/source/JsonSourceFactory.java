package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.parser.*;
import org.gensokyo.data.calcite.runtime.EffectiveExecutionPolicy;

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
        return create(name, source, null);
    }

    /**
     * Creates a row source, using {@link ChunkedJsonRowSource} when policy mode is {@code CHUNKED} or {@code STREAMING}.
     *
     * @param name   logical source name
     * @param source source configuration
     * @param policy optional effective execution policy; when {@code null}, uses in-memory {@link JsonRowSource}
     * @return row source implementation
     */
    public RowSource create(String name, SourceVO source, EffectiveExecutionPolicy policy) {
        JsonSourceVO jsonSource = (JsonSourceVO) source;
        if (policy != null && usesChunkedRead(policy.mode())) {
            return new ChunkedJsonRowSource(name, jsonSource, jsonParser, policy.fileSourceChunkSize());
        }
        return new JsonRowSource(name, jsonSource, jsonParser);
    }

    private static boolean usesChunkedRead(String mode) {
        return "CHUNKED".equals(mode) || "STREAMING".equals(mode);
    }
}
