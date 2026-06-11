/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import org.gensokyo.data.model.v2.TemplateV2DraftVO;

/**
 * Preview request carrying the in-memory draft from the wizard.
 *
 * @param draft                  current V2 draft from the wizard
 * @param maxRows                optional row cap; when null, uses draft execution policy or service default
 * @param throughTransformIndex  optional 0-based inclusive linear transformer index
 * @param computeBlockId         optional compute block id for DAG staged preview
 * @param throughTransformNodeId optional DAG node id inclusive cutoff
 * @author Gensokyo
 * @since 2026-05-26
 */
public record DraftPreviewRequest(
        TemplateV2DraftVO draft,
        Integer maxRows,
        Integer throughTransformIndex,
        String computeBlockId,
        String throughTransformNodeId) {
}
