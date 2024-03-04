/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.po;

import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.constant.StageType;
import org.gensokyo.data.constant.WriterType;

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
    /**
     * 写入器类型
     */
    private WriterType writerType;
    /**
     * 数据源编号
     */
    private String dataSourceId;
    /**
     * 写入目标对象
     */
    private String target;
    /**
     * 写入模板
     */
    private String template;

    @Override
    public StageType getType() {
        return StageType.WRITE;
    }

    @Override
    public void setType(StageType type) {
        super.setType(StageType.WRITE);
    }
}
