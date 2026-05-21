/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.qo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * Request body for Template V2 control-plane validation.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@Data
public class ValidateTemplateQO implements Serializable {

    @NotBlank(message = "Template YAML must not be blank")
    private String yaml;
}
