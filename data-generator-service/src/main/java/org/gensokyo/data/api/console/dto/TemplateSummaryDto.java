/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import org.gensokyo.data.model.po.TemplatePO;

/**
 * Template row for the operator catalog grid.
 *
 * @author Gensokyo
 * @since 2026-05-26
 */
public record TemplateSummaryDto(Long id, String name, Boolean archived) {

    /**
     * @param entity persisted template row
     * @return API DTO
     */
    public static TemplateSummaryDto from(TemplatePO entity) {
        return new TemplateSummaryDto(entity.getId(), entity.getName(), entity.getArchived());
    }
}
