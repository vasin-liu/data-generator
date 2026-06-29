package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.parser.*;
import org.gensokyo.data.calcite.runtime.EffectiveExecutionPolicy;

import org.gensokyo.data.model.v2.CsvSourceVO;
import org.gensokyo.data.model.v2.SourceVO;

public class CsvSourceFactory implements V2SourceFactory {

    private final CsvParser csvParser;

    public CsvSourceFactory() {
        this(new DefaultCsvParser());
    }

    public CsvSourceFactory(CsvParser csvParser) {
        this.csvParser = csvParser;
    }

    @Override
    public boolean supports(SourceVO source) {
        return source instanceof CsvSourceVO;
    }

    @Override
    public RowSource create(String name, SourceVO source) {
        return create(name, source, null);
    }

    /**
     * Creates a row source, using {@link ChunkedCsvRowSource} when policy mode is {@code CHUNKED} or {@code STREAMING}.
     *
     * @param name   logical source name
     * @param source source configuration
     * @param policy optional effective execution policy; when {@code null}, uses in-memory {@link CsvRowSource}
     * @return row source implementation
     */
    public RowSource create(String name, SourceVO source, EffectiveExecutionPolicy policy) {
        CsvSourceVO csvSource = (CsvSourceVO) source;
        if (policy != null && usesChunkedRead(policy.mode())) {
            return new ChunkedCsvRowSource(name, csvSource, csvParser, policy.fileSourceChunkSize());
        }
        return new CsvRowSource(name, csvSource, csvParser);
    }

    private static boolean usesChunkedRead(String mode) {
        return "CHUNKED".equals(mode) || "STREAMING".equals(mode);
    }
}
