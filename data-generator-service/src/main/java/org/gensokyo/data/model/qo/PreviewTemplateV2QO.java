/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.qo;

import lombok.Data;

import java.io.Serializable;

/**
 * Optional row cap for Template V2 control-plane preview.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
@Data
public class PreviewTemplateV2QO implements Serializable {

    private int maxRows = 50;
}
