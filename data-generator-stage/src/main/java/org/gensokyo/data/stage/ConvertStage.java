/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.converter.Converter;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.json.JsonKit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import java.util.Objects;

/**
 * 数据转换阶段
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
public class ConvertStage extends AbstractStage<ConvertStageVO> {

    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    public void setBeanFactory(AutowireCapableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public ConvertStage(StageContext<ConvertStageVO> ctx) {
        super(ctx);
    }

    @Override
    public Value internalExecute(Value input) {
        var spo = ctx.stage();
        try {
            var clazz = spo.getConverterType();
            if (Objects.nonNull(clazz)) {
                Converter converter = beanFactory.getBean(ctx.stage().getConverterType());
                return converter.convert(input);
            }
            return input;
        } catch (Exception e) {
            var msg = String.format("执行元素值类型转换阶段失败，输入值为：%s，上下文信息为：%s", input, JsonKit.write(ctx));
            throw new DataGeneratorException(msg, e);
        }
    }
}
