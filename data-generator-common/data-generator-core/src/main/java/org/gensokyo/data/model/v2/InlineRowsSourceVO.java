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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static multi-column rows embedded in a template (V2-native replacement for V1 constant reader / inline tables).
 *
 * @author Gensokyo
 * @since 2026-06-07
 */
@Getter
@Setter
@AutoService(SourceVO.class)
@JsonSubType("INLINE_ROWS")
public class InlineRowsSourceVO extends SourceVO {

    /**
     * Creates an inline-rows source with {@code type} set to {@code inline_rows}.
     */
    public InlineRowsSourceVO() {
        setType("inline_rows");
    }

    /** Optional declared schema; inferred from the first row when omitted. */
    private RowSchema schema;

    /** Materialized row payloads keyed by column name. */
    private List<Map<String, Object>> rows = new ArrayList<>();

    /**
     * @return defensive copy of row maps for runtime materialization
     */
    public List<Map<String, Object>> rowMaps() {
        List<Map<String, Object>> copies = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            copies.add(row == null ? new LinkedHashMap<>() : new LinkedHashMap<>(row));
        }
        return copies;
    }
}
