/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.source;

import org.gensokyo.data.calcite.RowSource;
import org.gensokyo.data.model.v2.MaterializationPolicyVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.util.List;

/**
 * Row source decorator that applies {@link MaterializationPolicyVO} after delegate materialization.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
public class MaterializationPolicyRowSource implements RowSource {
    private final RowSource delegate;
    private final List<Row> rows;

    /**
     * Wraps a delegate source and applies the given materialization policy to its rows.
     *
     * @param delegate underlying row source
     * @param policy   materialization policy to apply
     */
    public MaterializationPolicyRowSource(RowSource delegate, MaterializationPolicyVO policy) {
        this.delegate = delegate;
        this.rows = MaterializationPolicySupport.apply(delegate.rows(), policy);
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public RowSchema schema() {
        return delegate.schema();
    }

    @Override
    public List<Row> rows() {
        return rows;
    }
}
