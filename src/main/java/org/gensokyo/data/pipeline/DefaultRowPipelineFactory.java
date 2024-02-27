/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.constant.StageType;
import org.gensokyo.data.context.FieldContext;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.context.TemplateContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.FieldPO;
import org.gensokyo.data.po.TemplatePO;
import org.gensokyo.data.po.stage.ReadStagePO;
import org.gensokyo.data.po.stage.SelectStagePO;
import org.gensokyo.data.po.stage.StagePO;
import org.gensokyo.data.stage.SelectStage;
import org.gensokyo.data.stage.Stage;
import org.gensokyo.data.stage.StageFactory;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.MapValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.CollectKit;
import org.gensokyo.kit.json.JsonKit;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 字段数据生成流水线工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/23 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultRowPipelineFactory implements PipelineFactory {
    private final StageFactory stageFactory;

    @Override
    public Value startup(TemplateContext ctx) {
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        Assert.notNull(ctx.template().getTable(), "数据生成模板表配置不能为空");
        Assert.isTrue(CollectKit.isNotEmpty(ctx.template().getTable().getFields()), "数据生成模板表字段配置不能为空");
        //字段排序
        var orderedFields = sort(ctx.template());
        //最终生成结果行
        var result = new MapValue(16);
        //获取依赖字段集合
        var cache = getDependedFields(orderedFields);
        for (FieldPO field : orderedFields) {
            //检查字段是否配置正确
            checkRequired(field);

            final Value val;
            final var stageCtx = FieldContext.from(ctx, field);
            if (CollectKit.isEmpty(field.getDependsOn())) {
                //非依赖字段数据生成
                val = independentFieldProcessing(stageCtx, cache);
            } else {
                //依赖其他字段结果的字段生成
                val = dependentFieldProcessing(stageCtx, cache);
            }
            result.put(field.getName(), val);
        }
        return result;
    }

    @Override
    public void cleanup(TemplateContext ctx) {
        // nothing to do
    }

    private void checkRequired(FieldPO field) {
        var stages = field.getStages();
        if (CollectKit.isEmpty(field.getDependsOn()) && (
                CollectKit.isEmpty(stages)
                        || stages.stream().noneMatch(stage -> StageType.READ.equals(stage.getType()))
        )) {
            throw new DataGeneratorException(String.format("非依赖字段字段 %s 至少需要配置一个数据读取阶段", field.getName()));
        }

        var readStages = stages.stream()
                .filter(spo -> StageType.READ.equals(spo.getType()))
                .map(ReadStagePO.class::cast).toList();

        if (readStages.stream().filter(r -> StrKit.isEmpty(r.getDataSetId())).count() > 1) {
            throw new DataGeneratorException(String.format("字段 %s 数据有多个读取阶段时，必需指定唯一的数据集ID [dataSetId]，请检查配置是否正确", field.getName()));
        }

        readStages.forEach(rpo -> {
            if (StrKit.isEmpty(rpo.getDataSetId())) {
                //没有设置数据集ID，则设置为当前字段名称
                rpo.setDataSetId(field.getName());
            }
        });

        //检查字段是否包含数据选择阶段，如果没有，则添加一个默认的选择阶段，并且打印告警日志
        if (stages.stream().noneMatch(stage -> StageType.SELECT.equals(stage.getType()))) {
            log.warn("字段 {} 没有配置数据选择阶段，已自动添加默认的选择阶段", field.getName());
            var stage = new SelectStagePO();
            stage.setType(StageType.SELECT);
            field.getStages().add(stage);
        }
    }

    private MapValue getDependedFields(List<FieldPO> orderedFields) {
        var cache = new MapValue(16);
        orderedFields.forEach(f -> {
            if (CollectKit.isNotEmpty(f.getDependsOn())) {
                f.getDependsOn().forEach(name -> cache.put(name, Value.EMPTY));
            }
        });
        return cache;
    }

    private Value independentFieldProcessing(FieldContext ctx, MapValue cache) {
        return createPipelineAndExecute(ctx, cache, Value.EMPTY);
    }

    private Value dependentFieldProcessing(FieldContext ctx, MapValue cache) {
        //生成数据 依赖字段处理
        var field = ctx.field();
        var dds = ListValue.fromValueCollection(field.getDependsOn().stream().map(cache::get).toList());
        if (dds.isNullOrEmpty()) {
            throw new DataGeneratorException(
                    String.format("当前字段 %s 依赖的字段 %s 未在当前数据集中找到，请检查配置是否正确",
                            field.getName(), field.getDependsOn())
            );
        }
        if (dds.size() == 1) {
            dds = RandomKit.choiceOne(dds);
        }
        //依赖其他字段结果的字段
        return createPipelineAndExecute(ctx, cache, dds);
    }

    private Value createPipelineAndExecute(FieldContext ctx, MapValue cache, Value input) {
        var pipeline = new DefaultFieldPipeline();
        for (StagePO spo : ctx.field().getStages()) {
            var stageCtx = new StageContext<>(ctx.template(), ctx.field(), spo);
            var stage = stageFactory.newInstance(stageCtx);
            addListener(stageCtx, stage, cache);
            pipeline.next(stage);
        }
        return pipeline.execute(input);
    }

    private void addListener(StageContext<?> ctx, Stage stage, MapValue cache) {
        var fn = ctx.field().getName();
        if (stage instanceof SelectStage selectStage && (cache.containsKey(fn))) {
            //选择后的结果
            selectStage.onDone(output -> {
                cache.put(fn, output);
                log.debug("当前字段 {} 选择后的结果为 {}", fn, JsonKit.write(output));
            });
        }
    }

    /**
     * 对字段进行排序
     *
     * @param template 模板配置
     * @return 排序后的字段列表
     */
    private List<FieldPO> sort(final TemplatePO template) {
        var orderedFields = new ArrayList<FieldPO>();
        var fields = template.getTable().getFields();
        var dag = new DirectedAcyclicGraph<FieldPO, DefaultEdge>(DefaultEdge.class);
        var fieldMap = fields.stream().collect(Collectors.toMap(FieldPO::getName, field -> field));
        for (var field : fields) {
            dag.addVertex(field);
            if (CollectKit.isNotEmpty(field.getDependsOn())) {
                for (var fn : field.getDependsOn()) {
                    var df = fieldMap.get(fn);
                    if (Objects.nonNull(df)) {
                        dag.addVertex(df);
                        dag.addEdge(df, field);
                    } else {
                        throw new DataGeneratorException(
                                String.format("当前字段 %s 依赖的字段 %s 未在当前模板 %s 的配置表中找到，请检查配置是否正确",
                                        field.getName(), fn, template.getName())
                        );
                    }
                }
            }
        }
        var it = new TopologicalOrderIterator<>(dag);
        it.forEachRemaining(orderedFields::add);
        return orderedFields;
    }


    @Override
    public void shutdown(final TemplateContext ctx) {
        this.cleanup(ctx);
    }
}
