/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.vo.stage;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.scripter.PlainScriptVO;
import org.gensokyo.data.model.vo.scripter.ScriptVO;

/**
 * 脚本阶段配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/28 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(StageVO.class)
@JsonSubType(value = Const.StageType.SCRIPT)
public class ScriptStageVO extends StageVO {

    private ScriptVO language;

    public ScriptStageVO() {
        this.setType(Const.StageType.SCRIPT);
        this.language = new PlainScriptVO();
    }

    public ScriptStageVO(ScriptVO spo) {
        this.setType(Const.StageType.SCRIPT);
        this.language = spo;
    }
}
