/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import java.io.Serializable;

/**
 * Structured, actionable description of a transform/UDF failure in a Template V2 run (D-08).
 *
 * <p>Carries the failing transform step path, the operator type/name, the root-cause message, and
 * optional row/field locators so an operator can pinpoint and fix the template YAML. The {@code message}
 * is expected to already be sanitized by the throwing factory (no raw masked/PII values — see the mask
 * and json operators in Phase 4 04-02); this VO never re-introduces row payloads.</p>
 *
 * @param step         failing transform step path (e.g. {@code transformers[2]} or a compute-block path)
 * @param operatorType operator type ({@code json}/{@code mask}/{@code lookup}/{@code sql}/UDF type)
 * @param operatorName configured transform name, or {@code null} when unnamed
 * @param message      sanitized root-cause message
 * @param row          row ordinal when resolvable, or {@code null}
 * @param column       field/column locator when resolvable, or {@code null}
 * @author Gensokyo
 * @since 2026-06-22
 */
public record TransformErrorVO(
        String step,
        String operatorType,
        String operatorName,
        String message,
        Long row,
        String column) implements Serializable {
}
