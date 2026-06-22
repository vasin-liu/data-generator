/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.transform;

import org.gensokyo.data.calcite.V2TransformFactory;
import org.gensokyo.data.calcite.sql.CalciteExecutionContext;
import org.gensokyo.data.calcite.sql.CalciteRowTransformer;
import org.gensokyo.data.model.v2.MaskRuleVO;
import org.gensokyo.data.model.v2.MaskTransformVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.TransformVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Row-local transform that redacts column values in place using predefined named strategies.
 *
 * <p>Output schema equals the input schema (no columns added). Strategies (D-03):</p>
 * <ul>
 *   <li>{@code email} — keep the first character of the local part and the full domain, mask the rest
 *       (e.g. {@code j***@example.com}).</li>
 *   <li>{@code phone} — keep the last 4 alphanumeric characters, mask the rest.</li>
 *   <li>{@code credit-card} — keep the last 4 alphanumeric characters, mask the rest; separators preserved.</li>
 *   <li>{@code generic-fixed} — mask every alphanumeric character, preserving non-alphanumeric separators.</li>
 * </ul>
 *
 * <p>Failures are fail-fast (D-10) and PII-safe: exception messages carry the column and strategy names only,
 * never the original unmasked value. Null/blank cell values pass through unchanged.</p>
 *
 * @author Gensokyo
 * @since 2026-06-22
 */
public class MaskTransformFactory implements V2TransformFactory {

    private static final String INPUT_TABLE = "input";
    private static final int KEEP_LAST = 4;

    /**
     * Returns whether this factory handles {@link MaskTransformVO}.
     *
     * @param transform transform configuration
     * @return {@code true} for mask transforms
     */
    @Override
    public boolean supports(TransformVO transform) {
        return transform instanceof MaskTransformVO;
    }

    /**
     * Applies each masking rule in place over every row in table {@code input}.
     *
     * @param transform mask transform definition
     * @param context   execution context containing table {@code input}
     * @return input schema and rows with masked column values
     * @throws IllegalArgumentException if table {@code input} is missing or a rule names an unknown strategy
     */
    @Override
    public CalciteRowTransformer.TransformResult apply(TransformVO transform, CalciteExecutionContext context) {
        MaskTransformVO maskTransform = (MaskTransformVO) transform;
        RowSchema inputSchema = context.getSchemas().get(INPUT_TABLE);
        List<Row> inputRows = context.getData().get(INPUT_TABLE);
        if (inputSchema == null || inputRows == null) {
            throw new IllegalArgumentException("Mask transform requires table '" + INPUT_TABLE + "' in execution context");
        }

        List<MaskRuleVO> rules = maskTransform.getRules();
        List<Row> outputRows = new ArrayList<>(inputRows.size());
        for (Row inputRow : inputRows) {
            Map<String, Object> values = new LinkedHashMap<>(inputRow.values());
            for (MaskRuleVO rule : rules) {
                if (rule.getColumn() == null || rule.getColumn().isBlank()) {
                    throw new IllegalArgumentException("Mask rule requires a column");
                }
                String key = rule.getColumn().toLowerCase(Locale.ROOT);
                Object value = values.get(key);
                if (value == null || value.toString().isBlank()) {
                    continue;
                }
                values.put(key, mask(value.toString(), rule.getStrategy(), rule.getColumn()));
            }
            outputRows.add(new Row(values));
        }
        // Masking replaces values in place: the output schema is identical to the input schema.
        return new CalciteRowTransformer.TransformResult(inputSchema, outputRows);
    }

    private static String mask(String value, String strategy, String column) {
        if (strategy == null || strategy.isBlank()) {
            throw new IllegalArgumentException("Mask rule for column '" + column + "' requires a strategy");
        }
        return switch (strategy.toLowerCase(Locale.ROOT)) {
            case "email" -> maskEmail(value);
            case "phone", "credit-card" -> maskKeepLast(value, KEEP_LAST);
            case "generic-fixed" -> maskKeepLast(value, 0);
            // Unknown strategy fails fast (D-10); never echo the raw value (PII-safe).
            default -> throw new IllegalArgumentException("Unknown mask strategy: " + strategy);
        };
    }

    private static String maskEmail(String value) {
        int at = value.indexOf('@');
        if (at <= 0) {
            // No usable local part — fall back to fully masking alphanumerics.
            return maskKeepLast(value, 0);
        }
        String local = value.substring(0, at);
        String domain = value.substring(at);
        String maskedLocal = local.charAt(0) + "*".repeat(Math.max(local.length() - 1, 0));
        return maskedLocal + domain;
    }

    private static String maskKeepLast(String value, int keep) {
        int alphanumeric = 0;
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetterOrDigit(value.charAt(i))) {
                alphanumeric++;
            }
        }
        int maskCount = Math.max(alphanumeric - keep, 0);
        StringBuilder masked = new StringBuilder(value.length());
        int seen = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                seen++;
                masked.append(seen <= maskCount ? '*' : ch);
            } else {
                masked.append(ch);
            }
        }
        return masked.toString();
    }
}
