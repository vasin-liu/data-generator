/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.stage.MappingStagePO;
import org.gensokyo.data.util.DatasetKit;
import org.gensokyo.data.value.SingleValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.collect.MapKit;
import org.gensokyo.kit.json.JsonKit;

import java.util.Objects;

/**
 * 数据映射阶段
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
public class MappingStage extends AbstractStage<MappingStagePO> {

    public MappingStage(StageContext<MappingStagePO> ctx) {
        super(ctx);
    }

    @Override
    public Value internalExecute(Value input) {
        if (!(input instanceof SingleValue sv)) {
            throw new DataGeneratorException("当前输入值不是单值类型，不支持映射操作。");
        }
        var mpo = ctx.stage();
        try {
            if (MapKit.isNotEmpty(mpo.getMapping())) {
                var val = Objects.toString(sv.get());
                return DatasetKit.toValue(mpo.getMapping().get(val));
            }
            // 如果没有配置映射，则返回默认值，默认值如果为空，则返回输入值
            return Objects.isNull(mpo.getDefaultValue()) ? input : DatasetKit.toValue(mpo.getDefaultValue());
        } catch (Exception e) {
            throw new DataGeneratorException(String.format("字段 %s 的执行元素值映射阶段失败 ，输入值为：%s。",
                    ctx.field().getName(), JsonKit.write(input.get())), e);
        }
    }
}
