/*
 * Copyright © 2025 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.scripter.ScriptVO;
import org.gensokyo.data.model.vo.stage.StageVO;

import java.time.temporal.ChronoUnit;

/**
 * 暂停阶段配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2025/4/6 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(StageVO.class)
@JsonSubType(value = "PAUSE")
public class PauseStageVO extends StageVO {

    /**
     * 暂停时间，脚本配置同时存在时，以duration为准
     */
    private Integer duration;

    /**
     * 脚本配置
     */
    private ScriptVO language;

    /**
     * 暂停时长单位
     */
    private ChronoUnit unit = ChronoUnit.SECONDS;
}
