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
        for (FieldPO field : orderedFields) {
            //生成数据
            var pipeline = new DefaultFieldPipeline();
            var scriptStage = new ScriptStage(scriptFactory, field.getPostScript());
            if (CollectKit.isEmpty(field.getDependsOn())) {
                //非依赖字段
                var ds = fieldDatasets.get(field.getName());
                if (Objects.nonNull(ds) && !ds.isNullOrEmpty()) {
                    var result = pipeline
                            .next(new SelectStage())
                            .next(scriptStage)
                            .execute(ds);
                    //单个字段生成结果
                    r.put(field.getName(), result);
                } else {
                    log.error("当前字段 [{}] 未在当前数据集中找到，请检查配置是否正确", field.getName());
                }
            } else {
                var dds = ListValue.fromValueList(field.getDependsOn().stream().map(r::get).toList());
                //依赖其他字段结果的字段
                var result = pipeline
                        .next(new SelectStage())
                        .next(scriptStage)
                        .execute(dds);
                //依赖字段最终生成结果
                r.put(field.getName(), result);
            }
        }
        return r;
    }

    private MapValue fieldDatasets(final Context ctx) {
        var fds = ctx.dataset();

        if (fds instanceof MapValue mv) {
            return mv;
        } else {
            throw new DataGeneratorException("无效字段数据集类型，字段数据集类型必需为 MapValue 类型");
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
                        log.error("当前字段 [{}] 依赖的字段 [{}] 未在当前模板 [{}] 的配置表中找到，请检查配置是否正确",
                                field.getName(), fn, template.getName());
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
