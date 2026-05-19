/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.TemplateVO;

import java.util.Map;

/**
 * Executes V1 and V2 templates for migration compare, returning row counts and bounded samples.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public interface TemplateRunExecutor {

    /**
     * Runs a V1 template synchronously for compare sampling.
     *
     * @param v1     V1 template definition
     * @param params optional runtime parameters (may be empty)
     * @param options compare options (sample limit, execution hints)
     * @return row count and sample rows
     */
    RunOutcome runV1(TemplateVO v1, Map<String, Object> params, MigrationCompareOptions options);

    /**
     * Runs a V2 template synchronously for compare sampling.
     *
     * @param v2      normalized V2 template
     * @param params  optional runtime parameters (may be empty)
     * @param options compare options (sample limit, execution hints)
     * @return row count and sample rows
     */
    RunOutcome runV2(TemplateV2VO v2, Map<String, Object> params, MigrationCompareOptions options);
}
