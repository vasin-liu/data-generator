/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Rewrites V1 field SpEL ({@code #dataset}) into V2 {@link org.gensokyo.data.calcite.sql.SpelTransformFactory} form ({@code #row}).
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
public final class V1SpelExpressionRewriter {

    private static final Pattern DATASET_BRACKET = Pattern.compile("#dataset\\['([^']+)'\\]");
    private static final Pattern DATASET_DOT = Pattern.compile("#dataset\\.([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern DATASET_BARE = Pattern.compile("#dataset\\b");

    private V1SpelExpressionRewriter() {
    }

    /**
     * Rewrites a V1 script expression for use in {@link org.gensokyo.data.model.v2.SpelTransformVO}.
     *
     * @param expression V1 SpEL content (may use {@code #dataset})
     * @return expression using {@code #row} and {@code #row['col']} forms; {@code #faker} left unchanged
     */
    public static String rewrite(String expression) {
        if (expression == null || expression.isBlank()) {
            return expression;
        }
        // Bracket form before dot form so nested paths stay consistent.
        // V2 Calcite rows normalize column keys to lowercase (see QueryRowSourceSupport).
        String result = DATASET_BRACKET.matcher(expression).replaceAll(
                match -> "#row['" + match.group(1).toLowerCase(Locale.ROOT) + "']");
        result = DATASET_DOT.matcher(result).replaceAll(
                match -> "#row['" + match.group(1).toLowerCase(Locale.ROOT) + "']");
        result = DATASET_BARE.matcher(result).replaceAll("#row");
        return result;
    }
}
