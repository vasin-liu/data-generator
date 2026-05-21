/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import lombok.Data;

import java.io.Serializable;

/**
 * One output column produced by a row-local SpEL expression in {@link SpelTransformVO}.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@Data
public class SpelColumnMapping implements Serializable {

    /**
     * Output column name to add or replace on each row.
     */
    private String name;

    /**
     * SpEL expression evaluated per row, e.g. {@code "#row['id'] + '-x'"}.
     */
    private String expression;
}
