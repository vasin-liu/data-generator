/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.cache.DataCache;
import org.gensokyo.data.context.ReaderContext;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.exception.NotEnoughElementException;
import org.gensokyo.data.po.stage.ReadStagePO;
import org.gensokyo.data.read.ReaderFactory;
import org.gensokyo.data.read.strategy.ReaderSelectStrategyFactory;
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
public class ReadStage extends AbstractStage<ReadStagePO> {
    private ReaderFactory readerFactory;
    private ReaderSelectStrategyFactory readerSelectStrategyFactory;

    @Autowired
    public void setReaderFactory(ReaderFactory readerFactory) {
        this.readerFactory = readerFactory;
    }

    @Autowired
    public void setReaderSelectStrategyFactory(ReaderSelectStrategyFactory readerSelectStrategyFactory) {
        this.readerSelectStrategyFactory = readerSelectStrategyFactory;
    }

    public ReadStage(StageContext<ReadStagePO> ctx) {
        super(ctx);
    }

    @Override
    public Value internalExecute(Value input) {
        var rpo = ctx.stage();
        //如果数据集已经缓存，直接返回缓存数据集
        if (rpo.isInMemory()) {
            var tdc = DataCache.getOrCreate(ctx.template().getName());
            var ds = tdc.get(rpo.getDataSetId());
            if (Objects.isNull(ds)) {
                //缓存数据集为空，尝试从数据源读取数据集
                return tryReadFromDataSource(rpo, input);
            }
            //缓存数据集为空，抛出数据集为空异常
            if (ds.isNullOrEmpty()) {
                throw new NotEnoughElementException(String.format("字段 %s 的数据集 %s 已无足够可供选取的数据，请检查配置是否正确",
                        ctx.field().getName(), rpo.getDataSetId()));

            }
            log.debug("字段 {} 的数据集 {} 已缓存，直接返回缓存数据集", ctx.field().getName(), rpo.getDataSetId());
            //缓存数据集不为空，直接返回缓存数据集
            return ds;
        }

        return tryReadFromDataSource(rpo, input);
    }

    private Value tryReadFromDataSource(ReadStagePO rpo, Value input) {
        //重新读取数据集
        var result = new ListValue();
        var readerPo = readerSelectStrategyFactory.newInstance(rpo).select(rpo);
        try {
            var readerCtx = ReaderContext.from(ctx, readerPo);
            var ds = readerFactory.newInstance(readerPo).read(readerCtx, input);
            if (Objects.nonNull(ds) && !ds.isNullOrEmpty()) {
                result.addValue(ds);
            }
        } catch (Exception e) {
            throw new DataGeneratorException(String.format("字段 %s 的执行数据读取阶段失败，数据集读取类型为：%s ，输入值为：%s， 读取器配置为：%s。",
                    ctx.field().getName(), readerPo.getType(), JsonKit.write(input.get()), JsonKit.write(readerPo)), e);
        }
        Value val = DatasetKit.extractValue(result);
        //缓存数据集
        if (rpo.isInMemory()) {
            var tdc = DataCache.getOrCreate(ctx.template().getName());
            var cacheVal = tdc.get(rpo.getDataSetId());
            if (Objects.nonNull(cacheVal) && !cacheVal.isNullOrEmpty()) {
                throw new DataGeneratorException(String.format("当前字段 %s 读取到的数据集 %s 已存在于内存缓存中，请检查配置是否正确",
                        ctx.field().getName(), rpo.getDataSetId()));
            }
            tdc.set(rpo.getDataSetId(), val);
        }
        return val;
    }
}
