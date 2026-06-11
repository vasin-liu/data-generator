/*
 * Copyright © 2025 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.value.Value;

/**
 * 共享阶段
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2025/4/6 , Version 1.0.0
 */
public class SharedStage extends AbstractStage<SharedStageVO> {

    public SharedStage(StageContext<SharedStageVO> ctx) {
        super(ctx);
    }

    @Override
    public Value internalExecute(Value input) {
        //将当前值进行共享
        return input;
    }
}
