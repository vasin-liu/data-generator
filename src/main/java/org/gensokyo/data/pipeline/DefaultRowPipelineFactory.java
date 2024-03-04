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
import org.gensokyo.data.po.TemplatePO;
import org.gensokyo.data.stage.SelectStage;
import org.gensokyo.data.stage.Stage;
import org.gensokyo.data.stage.StageContext;
import org.gensokyo.data.stage.StageFactory;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.MapValue;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;
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
    public Value startup(Context ctx) {
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        Assert.notNull(ctx.template().getTable(), "数据生成模板表配置不能为空");
        Assert.isTrue(CollectKit.isNotEmpty(ctx.template().getTable().getFields()), "数据生成模板表字段配置不能为空");
        //字段排序
        var orderedFields = sort(ctx.template());
        //最终生成结果行
        var result = new MapValue(16);
        //获取依赖字段集合
        var dmv = getDependedFields(orderedFields);
        for (FieldPO field : orderedFields) {
            //检查字段是否配置正确
            checkRequired(field);

            final Value val;
            if (CollectKit.isEmpty(field.getDependsOn())) {
                //非依赖字段数据生成
                val = nonDependencyProduce(field, dmv);
            } else {
                //依赖其他字段结果的字段生成
                val = dependencyProduce(field, dmv);
            }
            result.put(field.getName(), val);
        }
        return result;
    }

    private void checkRequired(FieldPO field) {
        var stages = field.getStages();
        if (CollectKit.isEmpty(stages)
                || stages.stream().noneMatch(stage -> StageType.READ.equals(stage.getType()))) {
            throw new DataGeneratorException(String.format("字段 [%s] 至少需要配置一个数据读取阶段", field.getName()));
        }

        stages.stream()
                .filter(spo -> StageType.READ.equals(spo.getType()))
                .map(ReadStagePO.class::cast)
                .forEach(rpo -> {
                    var readers = rpo.getReaders();
                    if (CollectKit.isEmpty(readers)) {
                        throw new DataGeneratorException(String.format("字段 [%s] 数据读取阶段至少需要配置一个数据读取器", field.getName()));
                    }
                });
    }

    private MapValue getDependedFields(List<FieldPO> orderedFields) {
        var dmv = new MapValue(16);
        orderedFields.forEach(f -> {
            if (CollectKit.isNotEmpty(f.getDependsOn())) {
                f.getDependsOn().forEach(name -> dmv.put(name, Value.EMPTY));
            }
        });
        return dmv;
    }

    private Value nonDependencyProduce(FieldPO field, MapValue dmv) {
        //非依赖字段
        var pipeline = new DefaultFieldPipeline();
        for (StagePO spo : field.getStages()) {
            var stage = stageFactory.newInstance(new StageContext(spo));
            addListener(field, stage, dmv);
            pipeline.next(stage);
        }
        return pipeline.execute(Value.EMPTY);
    }

    private Value dependencyProduce(FieldPO field, MapValue dmv) {
        //生成数据
        var pipeline = new DefaultFieldPipeline();
        var dds = ListValue.fromValueList(field.getDependsOn().stream().map(dmv::get).toList());
        if (dds.isNullOrEmpty()) {
            throw new DataGeneratorException(
                    String.format("当前字段 [%s] 依赖的字段 [%s] 未在当前数据集中找到，请检查配置是否正确",
                            field.getName(), field.getDependsOn())
            );
        }
        //依赖其他字段结果的字段
        for (StagePO spo : field.getStages()) {
            pipeline.next(stageFactory.newInstance(new StageContext(spo)));
        }
        return pipeline.execute(dds);
    }

    private void addListener(FieldPO field, Stage stage, MapValue dmv) {
        if (stage instanceof SelectStage selectStage && (dmv.containsKey(field.getName()))) {
            //选择后的结果
            selectStage.onDone(output -> {
                dmv.put(field.getName(), output);
                log.debug("当前字段 [{}] 选择后的结果为 [{}]", field.getName(), JsonKit.write(output));
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
                                String.format("当前字段 [%s] 依赖的字段 [%s] 未在当前模板 [%s] 的配置表中找到，请检查配置是否正确",
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
    public void shutdown() {
        // nothing to do
    }
}
