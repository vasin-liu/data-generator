/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.util;

import org.gensokyo.data.generator.exception.DataGeneratorException;
import org.gensokyo.data.generator.function.Supplier;
import org.gensokyo.data.generator.function.TripleConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 数据生成分页工具
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/31 , Version 1.0.0
 */
public final class PageKit {

    private final int total;
    private final int size;
    private Supplier<Integer, List<Map<String, Object>>> supplier;
    private TripleConsumer<List<Map<String, Object>>, Integer, Integer, CompletableFuture<?>> consumer;

    private PageKit(int size, int total) {
        this.size = size;
        this.total = total;
    }

    public static PageKit of(int size, int total) {
        return new PageKit(size, total);
    }

    public PageKit supplier(Supplier<Integer, List<Map<String, Object>>> supplier) {
        this.supplier = supplier;
        return this;
    }

    public PageKit consumer(TripleConsumer<List<Map<String, Object>>, Integer, Integer, CompletableFuture<?>> consumer) {
        this.consumer = consumer;
        return this;
    }

    public List<CompletableFuture<?>> collect() {
        final List<CompletableFuture<?>> futures = new ArrayList<>();
        final var pages = getPages();
        for (int i = 1; i <= pages; i++) {
            final int bs;
            if (i == pages) {
                bs = lastPageSize(pages);
            } else {
                bs = this.size;
            }
            List<Map<String, Object>> data = supplier.get(bs);
            CompletableFuture<?> cf = consumer.accept(data, bs, i);
            futures.add(cf);
        }
        return futures;
    }

    private int lastPageSize(int pages) {
        if (pages * size < total) {
            throw new DataGeneratorException("当前并非最后一页");
        }
        if (pages * size == total) {
            return size;
        }
        if (pages * size > total) {
            return total - ((pages - 1) * size);
        }
        throw new IllegalStateException();
    }

    private int getPages() {
        if (size == 0) {
            return 0;
        }
        int pages = total / size;
        if (total % size != 0) {
            pages++;
        }
        return pages;
    }
}
