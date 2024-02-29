/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.Context;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.FieldPO;
import org.gensokyo.data.po.TemplatePO;
import org.gensokyo.data.script.ScriptFactory;
import org.gensokyo.data.stage.ScriptStage;
import org.gensokyo.data.stage.SelectStage;
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
    private final ScriptFactory scriptFactory;

    @Override
    public Value startup(Context ctx) {
        Assert.notNull(ctx.template(), "数据生成模板配置不能为空");
        Assert.notNull(ctx.template().getTable(), "数据生成模板表配置不能为空");
        Assert.isTrue(CollectKit.isNotEmpty(ctx.template().getTable().getFields()), "数据生成模板表字段配置不能为空");
        var orderedFields = sort(ctx.template());
        var fieldDatasets = fieldDatasets(ctx);
        var r = new MapValue(16);
        var dmv = getDependedFields(orderedFields);
        for (FieldPO field : orderedFields) {
            Value result;
            if (CollectKit.isEmpty(field.getDependsOn())) {
                var fds = fieldDatasets.get(field.getName());
                result = nonDependency(field, fds, dmv);
            } else {
                //依赖其他字段结果的字段
                result = dependency(field, dmv);
            }
            r.put(field.getName(), result);
        }
        return r;
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

    private Value nonDependency(FieldPO field, Value fds, MapValue dmv) {
        //非依赖字段
        var pipeline = new DefaultFieldPipeline();
        var scriptStage = new ScriptStage(scriptFactory, field.getPostScript());
        if (Objects.nonNull(fds) && !fds.isNullOrEmpty()) {
            var selectStage = new SelectStage();
            //该字段被其他字段依赖
            if (dmv.containsKey(field.getName())) {
                //选择后的结果
                selectStage.onDone(output -> {
                    dmv.put(field.getName(), output);
                    log.debug("当前字段 [{}] 选择后的结果为 [{}]", field.getName(), JsonKit.write(output));
                });
            }
            return pipeline
                    .next(selectStage)
                    .next(scriptStage)
                    .execute(fds);
        } else {
            log.error("当前字段 [{}] 未在当前数据集中找到，请检查配置是否正确", field.getName());
        }
        return Value.EMPTY;
    }

    private Value dependency(FieldPO field, MapValue dmv) {
        //生成数据
        var pipeline = new DefaultFieldPipeline();
        var scriptStage = new ScriptStage(scriptFactory, field.getPostScript());
        var dds = ListValue.fromValueList(field.getDependsOn().stream().map(dmv::get).toList());
        if (dds.isNullOrEmpty()) {
            var msg = String.format("当前字段 [%s] 依赖的字段 [%s] 未在当前数据集中找到，请检查配置是否正确", field.getName(), field.getDependsOn());
            throw new DataGeneratorException(msg);
        }
        //依赖其他字段结果的字段
        var selectStage = new SelectStage();
        selectStage.onDone(output -> log.debug("当前字段 [{}] 选择后的结果为 [{}]", field.getName(), JsonKit.write(output)));
        return pipeline
                .next(scriptStage)
                .next(scriptStage)
                .execute(dds);
    }

    private MapValue fieldDatasets(final Context ctx) {
        var fds = ctx.dataset();

        if (fds instanceof MapValue mv) {
            return mv;
        } else {
            var msg = String.format("无效字段数据集类型，字段数据集类型必需为 MapValue 类型，当前类型为 [%s]", fds.getClass().getName());
            throw new DataGeneratorException(msg);
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
                        var msg = String.format("当前字段 [%s] 依赖的字段 [%s] 未在当前模板 [%s] 的配置表中找到，请检查配置是否正确",
                                field.getName(), fn, template.getName());
                        throw new DataGeneratorException(msg);
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
