/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.WriteStagePO;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.MapValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.data.write.WriterFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 数据写入阶段类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
public class WriteStage extends AbstractStage {
    private WriterFactory writerFactory;

    public WriteStage(StageContext ctx) {
        super(ctx);
    }

    @Autowired
    public void setWriterFactory(WriterFactory writerFactory) {
        this.writerFactory = writerFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Value internalExecute(Value input) {
        if (Objects.isNull(input) || input.isNullOrEmpty()) {
            return Value.EMPTY;
        }
        var data = convert(input);
        if (ctx.stage() instanceof WriteStagePO wpo) {
            long rows = writerFactory.newInstance(wpo).write(data);
            log.info("数据写入完成，数据源ID为：{}，目标表为：{}，写入行数：{}。",
                    wpo.getDataSourceId(), wpo.getTarget(), rows);
        } else {
            throw new DataGeneratorException(String.format("当前阶段要求的配置值类型为：[%s] ，实际的配置值类型为：[%s]",
                    WriteStagePO.class.getName(), ctx.stage().getClass().getName()));
        }


        return input;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> convert(Value input) {
        if (input instanceof ListValue lv) {
            Value el = lv.first();
            if (el instanceof MapValue) {
                return (List<Map<String, Object>>) lv.get();
            } else {
                throw new DataGeneratorException(String.format("不支持的元素类型：%s", el.getClass().getName()));
            }
        } else {
            throw new DataGeneratorException(String.format("当前阶段要求的输入值类型为：[%s] ，实际的输入值类型为：[%s]",
                    ListValue.class.getName(), input.getClass().getName()));
        }
    }

}
