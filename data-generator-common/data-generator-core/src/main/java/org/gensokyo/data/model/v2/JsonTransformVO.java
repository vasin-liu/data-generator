/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;

/**
 * Built-in Template V2 transform that parses a JSON string column into an object and optionally
 * flattens its nested fields into separate columns using a separator-named convention
 * (e.g. {@code addr.city}). Both parse-only and parse-plus-flatten usages are expressed by one
 * operator; flatten is opt-in.
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
@Getter
@Setter
@AutoService(TransformVO.class)
@JsonSubType("JSON")
public class JsonTransformVO extends TransformVO {

    /**
     * Creates a transform with {@code type} set to {@code json}.
     */
    public JsonTransformVO() {
        setType("json");
    }

    /**
     * Input column whose string value is parsed as JSON on each input row.
     */
    private String sourceColumn;

    /**
     * Target column holding the parsed object when {@link #flatten} is {@code false}; nullable.
     */
    private String targetColumn;

    /**
     * When {@code true}, nested fields are flattened into separate output columns; defaults to {@code false}.
     */
    private boolean flatten = false;

    /**
     * Join separator used to compose nested-key column names when {@link #flatten} is {@code true};
     * defaults to a dot ({@code "."}).
     */
    private String separator = ".";
}
