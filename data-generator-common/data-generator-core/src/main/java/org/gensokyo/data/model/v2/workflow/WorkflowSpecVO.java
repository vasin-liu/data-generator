/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2.workflow;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Ordered L2 workflow definition attached to a {@link org.gensokyo.data.model.v2.TemplateV2VO}.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Getter
@Setter
@NoArgsConstructor
public class WorkflowSpecVO implements Serializable {
    private List<WorkflowStepVO> steps = new ArrayList<>();
}
