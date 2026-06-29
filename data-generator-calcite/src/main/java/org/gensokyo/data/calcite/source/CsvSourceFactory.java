package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.*;
import org.gensokyo.data.calcite.parser.*;
import org.gensokyo.data.calcite.runtime.EffectiveExecutionPolicy;

import org.gensokyo.data.model.v2.CsvSourceVO;
import org.gensokyo.data.model.v2.SourceVO;

public class CsvSourceFactory implements V2SourceFactory {
    private static final int GLOBAL_DEFAULT_SOURCE_CHUNK_SIZE = 5_000;

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
            return new ChunkedCsvRowSource(name, csvSource, csvParser, resolveCsvChunkSize(policy));
        }
        return new CsvRowSource(name, csvSource, csvParser);
    }

    private static int resolveCsvChunkSize(EffectiveExecutionPolicy policy) {
        // D-03: CSV chunked reads default to 1000 when template does not set sourceChunkSize.
        int size = policy.sourceChunkSize();
        if (size == GLOBAL_DEFAULT_SOURCE_CHUNK_SIZE) {
            return ChunkedCsvRowSource.DEFAULT_CSV_CHUNK_SIZE;
        }
        return size > 0 ? size : ChunkedCsvRowSource.DEFAULT_CSV_CHUNK_SIZE;
    }

    private static boolean usesChunkedRead(String mode) {
        return "CHUNKED".equals(mode) || "STREAMING".equals(mode);
    }
}
