/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.cache.DataSet;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.exception.NotEnoughElementException;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.reader.ReaderFactory;
import org.gensokyo.data.reader.strategy.ReaderSelectStrategyFactory;
import org.gensokyo.data.util.DatasetKit;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.json.JsonKit;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 * 数据读取阶段
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
public class ReadStage extends AbstractStage<ReadStageVO> {
    @Setter(onMethod_ = @Autowired)
    private ReaderFactory readerFactory;

    @Setter(onMethod_ = @Autowired)
    private ReaderSelectStrategyFactory readerSelectStrategyFactory;

    public ReadStage(StageContext<ReadStageVO> ctx) {
        super(ctx);
    }

    @Override
    public Value internalExecute(Value input) {
        var rpo = ctx.stage();
        //如果数据集已经缓存，直接返回缓存数据集
        if (rpo.isInMemory()) {
            var tdc = DataSet.getOrCreate(ctx.template().getId(), ctx.template().getInstanceId());
            var ds = tdc.get(rpo.getDataSetId());
            if (Objects.isNull(ds)) {
                //缓存数据集为空，尝试从数据源读取数据集
                var val = tryReadFromDataSource(rpo, input);
                tdc.set(rpo.getDataSetId(), val);
                return val;
            }

            //缓存数据集为空，抛出数据集为空异常
            if (ds.isNullOrEmpty()) {
                var msg = String.format("当前阶段的数据集 %s 已无足够可供选取的数据，上下文信息为：%s",
                        rpo.getDataSetId(), JsonKit.write(ctx));
                throw new NotEnoughElementException(msg);
            }

            //缓存数据集不为空，直接返回缓存数据集
            return ds;
        }

        return tryReadFromDataSource(rpo, input);
    }

    private Value tryReadFromDataSource(ReadStageVO rpo, Value input) {
        //重新读取数据集
        var result = new ListValue();
        var rctx = StageContext.from(ctx);
        var readerPo = readerSelectStrategyFactory.newInstance(rpo).select(rctx, rctx.stage().getStrategy());
        try {
            var readerCtx = StageContext.from(ctx);
            var ds = readerFactory.newInstance(readerPo).read(readerCtx, readerPo, input);
            if (Objects.nonNull(ds) && !ds.isNullOrEmpty()) {
                result.addValue(ds);
            }
        } catch (Exception e) {
            var msg = String.format("执行数据读取阶段失败，输入值为：%s，上下文信息为：%s", input, JsonKit.write(ctx));
            throw new DataGeneratorException(msg, e);
        }

        return DatasetKit.extractValue(result);
    }
}
