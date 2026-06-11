/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

import java.util.List;

/**
 * Raised when an L1 transform DAG is structurally invalid (missing nodes, unknown transform ids, cycles).
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public final class TransformDagValidationException extends IllegalArgumentException {

    private final List<String> cyclePath;

    /**
     * Creates a validation error without a cycle path.
     *
     * @param message human-readable failure description
     */
    public TransformDagValidationException(String message) {
        super(message);
        this.cyclePath = List.of();
    }

    /**
     * Creates a cycle detection error with the discovered node path.
     *
     * @param message human-readable failure description
     * @param cyclePath ordered node ids forming the cycle (first id repeated at end when closed)
     */
    public TransformDagValidationException(String message, List<String> cyclePath) {
        super(message);
        this.cyclePath = List.copyOf(cyclePath);
    }

    /**
     * Returns the cycle path when this failure is due to a cyclic graph; otherwise empty.
     *
     * @return cycle node ids, possibly empty
     */
    public List<String> getCyclePath() {
        return cyclePath;
    }
}
