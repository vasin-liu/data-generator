/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.sink;

import org.gensokyo.data.calcite.RowSink;

/**
 * File sink adapters that support per-chunk append during CHUNKED/STREAMING pipeline runs.
 *
 * @author Gensokyo
 * @since 2026-06-29
 */
public interface StreamingFileRowSink extends RowSink {

    /**
     * Enables streaming mode: first {@link #write} truncates/creates the file; later writes append.
     */
    void enableStreaming();
}
