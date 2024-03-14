/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po.stage;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.StageType;
import org.gensokyo.data.po.writer.WriterPO;

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
public class WriteStagePO extends StagePO {

    private List<WriterPO> writers;

    @Override
    public StageType getType() {
        return StageType.WRITE;
    }

    @Override
    public void setType(StageType type) {
        super.setType(StageType.WRITE);
    }
}
