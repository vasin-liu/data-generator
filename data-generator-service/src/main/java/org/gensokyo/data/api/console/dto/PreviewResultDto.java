/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.gensokyo.data.template.TemplateV2PreviewDTO;

/**
 * Preview outcome including resolved template id after implicit save.
 *
 * @author Gensokyo
 * @since 2026-05-26
 */
public record PreviewResultDto(
        @JsonSerialize(using = ToStringSerializer.class) Long templateId,
        TemplateV2PreviewDTO preview) {
}
