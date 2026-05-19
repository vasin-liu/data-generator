/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import java.util.List;
import java.util.Map;

/**
 * Bounded outcome of a template run used for migration dual-run compare.
 *
 * @param rowCount total rows produced by the run
 * @param sample   positional sample rows (maps of column name to value)
 * @author Gensokyo
 * @since 2026-05-19
 */
public record RunOutcome(long rowCount, List<Map<String, Object>> sample) {
}
