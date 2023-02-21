/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.dataset;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 可读数据集
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/29 , Version 1.0.0
 */
public class ReadableDataset implements Dataset, Readable {

    private final Supplier<List<Object>> supplier;
    private final boolean isLazy;

    private ReadableDataset(Supplier<List<Object>> supplier) {
        this.supplier = Objects.requireNonNull(supplier);
        this.isLazy = false;
    }

    private ReadableDataset() {
        this.supplier = Collections::emptyList;
        this.isLazy = true;
    }

    public static ReadableDataset lazy() {
        return new ReadableDataset();
    }

    public static ReadableDataset empty() {
        return new ReadableDataset(Collections::emptyList);
    }

    public static ReadableDataset of(List<Object> data) {
        return new ReadableDataset(() -> data);
    }

    public static ReadableDataset of(Supplier<List<Object>> supplier) {
        return new ReadableDataset(supplier);
    }

    @Override
    public List<Object> fetch() {
        return supplier.get();
    }

    @Override
    public boolean isLazy() {
        return isLazy;
    }
}
