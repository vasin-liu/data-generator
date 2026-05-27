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
 * @author Gensokyo
 * @since 2026-05-26
 */
public record DraftPreviewRequest(TemplateV2DraftVO draft, Integer maxRows) {
}
