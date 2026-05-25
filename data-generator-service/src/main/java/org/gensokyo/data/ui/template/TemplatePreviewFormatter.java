/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.template;

import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.template.TemplateV2PreviewDTO;

/**
 * Formats {@link TemplateV2PreviewDTO} for read-only display in the operator console.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
public final class TemplatePreviewFormatter {

    private TemplatePreviewFormatter() {
    }

    /**
     * @param preview control-plane preview result
     * @return human-readable summary for a dialog or text area
     */
    public static String format(TemplateV2PreviewDTO preview) {
        StringBuilder sb = new StringBuilder();
        if (preview.getWarnings() != null && !preview.getWarnings().isEmpty()) {
            sb.append("Warnings:\n");
            preview.getWarnings().forEach(w -> sb.append("  - ").append(w).append('\n'));
            sb.append('\n');
        }
        if (preview.getSchema() != null) {
            sb.append("Schema:\n").append(TemplateJsonCodec.write(preview.getSchema())).append("\n\n");
        }
        int rowCount = preview.getRows() != null ? preview.getRows().size() : 0;
        sb.append("Sample rows (").append(rowCount).append("):\n");
        if (preview.getRows() != null) {
            for (Row row : preview.getRows()) {
                sb.append(TemplateJsonCodec.write(row)).append('\n');
            }
        }
        return sb.toString();
    }
}
