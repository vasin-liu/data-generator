/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.script;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.model.vo.scripter.PlainScriptVO;
import org.gensokyo.data.model.vo.stage.ScriptStageVO;
import org.gensokyo.data.util.DatasetKit;
import org.gensokyo.data.value.Value;

/**
 * 原始内容
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/27 , Version 1.0.0
 */
public class PlainScript<S extends ScriptStageVO, T extends PlainScriptVO> implements Script<S, T> {

    @Override
    public Value eval(final StageContext<S> ctx, final T spo, Value dataset, Object... args) {
        return DatasetKit.toValue(spo.getContent());
    }
}
