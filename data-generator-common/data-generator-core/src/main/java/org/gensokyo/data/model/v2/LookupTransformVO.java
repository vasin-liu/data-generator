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

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in Template V2 transform that enriches input rows by joining, on a key, against another
 * source already declared in the same template (a named in-template source such as a csv or query
 * source). It does NOT read from a configured JDBC datasource and does NOT define inline maps.
 * Missing or duplicate join keys are fail-fast failures surfaced through the structured transform
 * error contract (implemented in plans 04-02 / 04-04).
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
@Getter
@Setter
@AutoService(TransformVO.class)
@JsonSubType("LOOKUP")
public class LookupTransformVO extends TransformVO {

    /**
     * Creates a transform with {@code type} set to {@code lookup}.
     */
    public LookupTransformVO() {
        setType("lookup");
    }

    /**
     * Name of the in-template named source to join against; keys into the execution-context table map.
     */
    private String source;

    /**
     * Input-row column used as the join key.
     */
    private String leftKey;

    /**
     * Lookup-source column matched against {@link #leftKey}.
     */
    private String rightKey;

    /**
     * Lookup-source columns projected onto the enriched output row.
     */
    private List<String> columns = new ArrayList<>();
}
