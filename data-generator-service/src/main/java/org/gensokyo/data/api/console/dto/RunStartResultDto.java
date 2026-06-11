/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * Async run submission result for template grid actions.
 *
 * @author Gensokyo
 * @since 2026-05-26
 */
public record RunStartResultDto(
        @JsonSerialize(using = ToStringSerializer.class) Long templateId,
        @JsonSerialize(using = ToStringSerializer.class) Long instanceId) {
}
