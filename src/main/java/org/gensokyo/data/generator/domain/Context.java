/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.domain;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据生成上下文对象
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/4 , Version 1.0.0
 */
public class Context implements Serializable {

    private final TemplatePO meta;
    private final Map<String, List<Object>> globalDataSet = new ConcurrentHashMap<>(8);

    public Context(TemplatePO meta) {
        this.meta = meta;
    }

    public TemplatePO meta() {
        return this.meta;
    }

    public Map<String, List<Object>> global() {
        return this.globalDataSet;
    }

    public Context global(String key, List<Object> value) {
        globalDataSet.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
        return this;
    }

    public List<Object> global(String key) {
        return globalDataSet.get(Objects.requireNonNull(key));
    }
}
