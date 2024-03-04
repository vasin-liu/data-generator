/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.Context;
import org.gensokyo.data.constant.StageType;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.FieldPO;
import org.gensokyo.data.po.ReadStagePO;
import org.gensokyo.data.po.StagePO;
import org.gensokyo.data.stage.StageContext;
import org.gensokyo.data.stage.StageFactory;
import org.gensokyo.data.util.DatetimeKit;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.MapValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;
import org.gensokyo.kit.collect.CollectKit;
import org.springframework.util.StopWatch;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 默认数据读取流水线工厂
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultReadPipelineFactory implements PipelineFactory {
    private final StageFactory stageFactory;

    @Override
    public Value startup(final Context ctx) {
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        Assert.notNull(ctx.template().getTable(), "数据生成模板表配置不能为空");
        Assert.isTrue(CollectKit.isNotEmpty(ctx.template().getTable().getFields()), "数据生成模板表字段配置不能为空");
        return loadTableDataset(ctx.template().getTable().getFields());
    }


    private Value loadTableDataset(final List<FieldPO> fields) {
        log.info("开始加载任务表字段数据集");
        var dataReady = new CountDownLatch(1);
        var stopWatch = new StopWatch();
        stopWatch.start();
        var fieldDataset = new MapValue(16);
        for (var field : fields) {
            if (CollectKit.isNotEmpty(field.getDependsOn())) {
                //依赖类型字段不需要进行数据读取，直接使用其依赖字段的选取结果
                fieldDataset.put(field.getName(), Value.EMPTY);
            } else {
                //其他非依赖型字段，通过读取器读取数据
                var readers = getReaders(field);
                if (CollectKit.isNotEmpty(readers)) {
                    var ds = readers.stream().map(this::read).toList();
                    //合并数据集
                    fieldDataset.put(field.getName(), merged(ds));
                }
            }
        }
        dataReady.countDown();
        stopWatch.stop();
        log.info("当前任务表字段数据集加载完成，总计耗时：{} ", DatetimeKit.humanized(stopWatch.getTotalTimeMillis()));
        return fieldDataset;
    }

    private List<ReadStagePO.ReaderPO> getReaders(FieldPO field) {
        var stages = field.getStages();
        if (CollectKit.isEmpty(stages)
                || stages.stream().noneMatch(stage -> StageType.READ.equals(stage.getType()))) {
            throw new DataGeneratorException(String.format("字段 [%s] 至少需要配置一个数据读取阶段", field.getName()));
        }
        return stages.stream()
                .filter(stage -> StageType.READ.equals(stage.getType()))
                .map(stage -> ((ReadStagePO) stage).getReaders())
                .flatMap(Collection::stream)
                .toList();
    }

    private Value read(final ReadStagePO.ReaderPO rpo) {
        var pipeline = new DefaultReadPipeline();
        for (StagePO spo : rpo.getStages()) {
            var ctx = new StageContext(spo);
            pipeline.next(stageFactory.newInstance(ctx));
        }
        return pipeline
                .onDone(output -> log.debug("数据源编号为：{}，数据集编号为：{} ，数据读取流水线执行完成", rpo.getDataSourceId(), rpo.getDataSetId()))
                //执行，数据读取一般情况下无需输入其他数据，因此传入空数据集
                .execute(Value.EMPTY);
    }

    private Value merged(final List<Value> ds) {
        var mergedDs = new ListValue(16);
        for (Value d : ds) {
            if (d instanceof ListValue lv) {
                mergedDs.addAll(lv.stream().toList());
            } else {
                mergedDs.add(d);
            }
        }
        return mergedDs;
    }

    @Override
    public void shutdown() {
        //nothing to do
    }
}
