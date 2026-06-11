/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Bounded preview of a Template V2 run: output schema, sample rows, and non-fatal warnings.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateV2PreviewDTO implements Serializable {

    private RowSchema schema;
    private List<Row> rows = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
