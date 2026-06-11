/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.model.vo.stage;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.json.JsonSubType;
import org.gensokyo.data.model.vo.writer.WriterVO;

import java.util.List;

/**
 * 数据写入阶段配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/28 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(StageVO.class)
@JsonSubType(value = Const.StageType.WRITE, isDefault = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public class WriteStageVO extends StageVO {

    private List<WriterVO> writers;

    @Override
    public String getType() {
        return Const.StageType.WRITE;
    }

    @Override
    public void setType(String type) {
        super.setType(Const.StageType.WRITE);
    }
}
