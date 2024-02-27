/*
 * Copyright © 2023 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * 数据集接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/10/24 , Version 1.0.0
 */
public class Dataset111 {

    private final CopyOnWriteArrayList<Supplier<Object>> data = new CopyOnWriteArrayList<>();
    private final boolean isLazy;

    private Dataset111(Builder builder) {
        this.data.addAll(builder.ds);
        this.isLazy = builder.lazy;
    }

    public int size() {
        return data.size();
    }

    public boolean onlyOneElement() {
        return size() == 1;
    }

    public boolean isLazy() {
        return isLazy;
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public List<Supplier<Object>> data() {
        return this.data;
    }

    public List<Object> toListObj() {
        return data.stream().map(Supplier::get).toList();
    }

    public Object first() {
        return toListObj().stream().findFirst().orElse(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Map<String, Object>> toListMap() {
        return data.stream().map(Supplier::get).map(o -> {
            if (o instanceof Map m) {
                Map<String, Object> r = new HashMap<>();
                m.forEach((key, value) -> {
                    /*if (value instanceof Supplier s) {
                        r.put((String) key, s.get());
                    } else*/
                    if (value instanceof Dataset111 d) {
                        r.put((String) key, d.toListObj());
                    } /*else {
                        r.put((String) key, value);
                    }*/
                });
                return r;
            }
            throw new UnsupportedOperationException("当前列表中的数据类型不支持转换为Map类型 ");
        }).toList();
    }

    public Dataset111 copy() {
        Builder builder = new Builder(this.data());
        if (isLazy()) {
            builder.lazy();
        }
        return builder.build();
    }

    public static Builder of(Object data) {
        return new Builder(data);
    }

    public static Dataset111 empty() {
        return of(List.of()).build();
    }

    public static Dataset111 lazy() {
        return of(List.of()).lazy().build();
    }

    public Dataset111 concat(Dataset111 other) {
        Dataset111 result = copy();
        if (Objects.isNull(other) || other.isEmpty()) {
            return result;
        }

        result.data().addAll(other.data);
        return result;
    }

    public Dataset111 combine(List<Dataset111> datasets) {
        if (Objects.isNull(datasets)) {
            return Dataset111.empty();
        }
        Dataset111 result = copy();
        for (Dataset111 ds : datasets) {
            if (Objects.nonNull(ds) && ds.isNotEmpty()) {
                result.data().addAll(ds.data());
            }
        }
        return result;
    }

    public Dataset111 clear() {
        this.data.clear();
        return this;
    }

    public static class Builder {

        private final CopyOnWriteArrayList<Supplier<Object>> ds = new CopyOnWriteArrayList<>();
        private boolean lazy = false;

        public Builder(Object data) {
            if (Objects.nonNull(data)) {
                add(data);
            }
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private void add(Object data) {
            if (data instanceof Dataset111 d1) {
                this.ds.addAll(d1.data());
            } else if (data instanceof Supplier s) {
                this.ds.add(s);
            } else if (data instanceof Collection c) {
                c.forEach(this::add);
            } else {
                this.ds.add(() -> data);
            }
        }

        public Builder lazy() {
            this.lazy = true;
            return this;
        }

        public Dataset111 build() {
            return new Dataset111(this);
        }
    }
}
