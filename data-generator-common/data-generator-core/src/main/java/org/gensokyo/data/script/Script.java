/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.script;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.model.vo.scripter.ScriptVO;
import org.gensokyo.data.model.vo.stage.ScriptStageVO;
import org.gensokyo.data.value.Value;

/**
 * 脚本接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@FunctionalInterface
public interface Script<S extends ScriptStageVO, T extends ScriptVO> {

    /**
     * 执行脚本
     *
     * @param spo     脚本阶段配置
     * @param dataset 输入数据集
     * @param args    输入参数列表
     * @return 脚本执行结果
     */
    Value eval(final StageContext<S> ctx, final T spo, final Value dataset, Object... args);
}
