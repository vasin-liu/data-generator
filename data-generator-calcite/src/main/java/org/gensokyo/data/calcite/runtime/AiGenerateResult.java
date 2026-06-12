/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.calcite.runtime;

/**
 * Payload and optional diagnostics returned from an AI runtime bridge call.
 *
 * @param payload parsed or raw provider output
 * @param metric  call diagnostics when tracing is enabled
 * @author Gensokyo
 * @since 2026-06-11
 */
public record AiGenerateResult(Object payload, AiCallMetric metric) {
}
