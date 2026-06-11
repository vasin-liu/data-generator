/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.condition.OtherwiseVO;
import org.gensokyo.data.model.vo.condition.WhenVO;
import org.gensokyo.data.model.vo.stage.StageVO;

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
@AutoService(StageVO.class)
@JsonSubType(value = "CONDITION")
public class ConditionStageVO extends StageVO {

    /**
     * 条件分支列表
     */
    private List<WhenVO> choose;

    /**
     * 其他条件
     */
    private OtherwiseVO otherwise;
}
