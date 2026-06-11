/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.model.v2.Row;

import java.util.List;

/**
 * Row source that reads JDBC query results in bounded chunks instead of materializing all rows.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public interface ChunkedRowSource extends RowSource {

    /**
     * Whether this source supports the chunked read API.
     *
     * @return {@code true} when {@link #nextChunk(int)} may be used
     */
    default boolean supportsChunking() {
        return true;
    }

    /**
     * Whether another chunk of rows is available.
     *
     * @return {@code true} if {@link #nextChunk(int)} may return more rows
     */
    boolean hasNextChunk();

    /**
     * Reads up to {@code maxRows} rows from the underlying cursor.
     *
     * @param maxRows maximum rows for this chunk (must be positive)
     * @return chunk of rows; may be smaller than {@code maxRows} at end of stream
     */
    List<Row> nextChunk(int maxRows);

    /**
     * Total rows returned by {@link #nextChunk(int)} so far in this source.
     *
     * @return cumulative row count
     */
    long rowsReadSoFar();
}
