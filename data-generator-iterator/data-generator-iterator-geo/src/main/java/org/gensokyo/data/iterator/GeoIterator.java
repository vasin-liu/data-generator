/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.iterator;

import org.gensokyo.data.context.IteratorContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.geo.GeoGenerationRequest;
import org.gensokyo.data.geo.GeoSyntheticGenerator;
import org.gensokyo.data.value.MapValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

/**
 * Iterator that emits synthetic geospatial rows.
 *
 * @author Gensokyo
 * @since 2026-05-20
 */
public class GeoIterator<T extends GeoIteratorVO> extends AbstractIterator<T> {

    private final Queue<Map<String, Object>> rows;

    public GeoIterator(IteratorContext<T> ctx) {
        super(ctx);
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        Assert.notNull(ctx.iterator(), "迭代器配置不能为空");
        GeoGenerationRequest request = GeoIteratorRequestMapper.toRequest(ctx.iterator());
        try {
            this.rows = new ArrayDeque<>(GeoSyntheticGenerator.generateRows(request));
        } catch (Exception e) {
            throw new DataGeneratorException("Failed to initialize GEO iterator", e);
        }
    }

    @Override
    public boolean hasNext() {
        return !rows.isEmpty();
    }

    @Override
    public Value next() {
        if (!hasNext()) {
            throw new IllegalStateException("迭代器已经到达最大值");
        }
        return MapValue.fromMap(rows.poll());
    }
}
