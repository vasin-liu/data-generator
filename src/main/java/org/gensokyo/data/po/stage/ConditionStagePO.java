/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po.stage;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.po.condition.Otherwise;
import org.gensokyo.data.po.condition.WhenPO;

import java.util.List;

/**
 * 条件分支阶段配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/27 , Version 1.0.0
 */
@Getter
@Setter
public class ConditionStagePO extends StagePO {

    /**
     * 条件分支列表
     */
    private List<WhenPO> choose;

    /**
     * 其他条件
     */
    private Otherwise otherwise;
}
