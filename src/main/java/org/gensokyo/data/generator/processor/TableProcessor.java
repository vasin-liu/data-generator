/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.processor;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.generator.Destroyable;
import org.gensokyo.data.generator.converter.StringConverter;
import org.gensokyo.data.generator.dataset.Dataset;
import org.gensokyo.data.generator.domain.Context;
import org.gensokyo.data.generator.domain.FieldPO;
import org.gensokyo.data.generator.domain.MapperPO;
import org.gensokyo.data.generator.domain.ResultMapperPO;
import org.gensokyo.data.generator.exception.DataGeneratorException;
import org.gensokyo.data.generator.factory.ConverterFactory;
import org.gensokyo.data.generator.factory.ScriptFactory;
import org.gensokyo.data.generator.script.Script;
import org.gensokyo.data.generator.util.RandomKit;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.CollectKit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表处理器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/9 , Version 1.0.0
 */
@Slf4j
public class TableProcessor implements Processor<List<FieldPO>, Map<String, Object>>, Destroyable {
    private ConverterFactory converterFactory;
    private ScriptFactory scriptFactory;
    private Context ctx;
    //FieldName,DataSet
    private Map<String, Dataset> fieldDataset;
    private Map<String, Script> preScriptCache = new ConcurrentHashMap<>(16);
    private Map<String, Script> postScriptCache = new ConcurrentHashMap<>(16);

    public TableProcessor(ConverterFactory converterFactory, ScriptFactory scriptFactory,
                          Context ctx, Map<String, Dataset> fieldDataset) {
        this.converterFactory = converterFactory;
        this.scriptFactory = scriptFactory;
        this.ctx = ctx;
        this.fieldDataset = fieldDataset;
    }

    @Override
    public Map<String, Object> handle(List<FieldPO> fields) {
        var row = new HashMap<String, Object>(16);
        var fieldResults = new HashMap<String, Object>(16);
        //根据DAG依赖关系排序后的字段集合
        for (var field : fields) {
            var dataset = fieldDataset.get(field.getName());
            try {
                Object result;
                if (dataset.isLazy()) {
                    //延迟加载的依赖数据集
                    //获取当前字段所有依赖字段的随机生成的结果集合
                    //注意：该结果取决于字段的DAG排序结果
                    var dv = getDependsValue(field, fieldResults);
                    //根据依赖字段的结果生成当前字段的值
                    result = handleDepends(field, dv);
                } else {
                    //非依赖数据集，随机选择一个值后使用前置脚本处理
                    result = preScript(field, RandomKit.choiceOne(dataset.fetch()));
                }
                if (Objects.nonNull(result)) {
                    //后置脚本处理之前，因此如果需要的是依赖字段脚本处理后的值，则需要在前置脚本进行处理
                    fieldResults.put(field.getName(), result);
                }
                //后置脚本处理
                var evalResult = postScript(field, result);
                //结果映射选择
                var mappingResult = resultMapping(field.getResultMapper(), evalResult);
                //类型转换
                var convertedResult = convert(field, mappingResult);
                //装载结果
                row.put(field.getName(), convertedResult);
            } catch (Exception e) {
                log.error(String.format("字段 [%s] 生成结果出现异常：", field.getName()), e);
                throw new DataGeneratorException(e);
            }
        }
        //clear cache
        fieldResults.clear();
        return row;
    }

    private Map<String, Object> getDependsValue(FieldPO field, Map<String, Object> fieldResults) {
        if (CollectKit.isEmpty(field.getDependsOn())) {
            throw new DataGeneratorException("字段依赖配置异常");
        }
        var r = new HashMap<String, Object>();
        for (var fn : field.getDependsOn()) {
            var v = fieldResults.get(fn);
            if (Objects.nonNull(v)) {
                r.put(fn, v);
            }
        }
        return r;
    }

    private Object handleDepends(FieldPO fpo, Map<String, Object> dataset) {
        if (Objects.nonNull(fpo.getPreScript())
            && StrKit.isNotBlank(fpo.getPreScript().getContent())
            && Objects.nonNull(fpo.getPreScript().getType())) {
            return preScript(fpo, dataset);
        }

        //无脚本处理且有多个依赖结果数据时，默认选取第一个不为空的结果作为当前字段的结果
        return dataset.values().stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Object preScript(FieldPO fpo, Object result) {
        try {
            var script = preScriptCache.get(fpo.getName());
            if (Objects.isNull(script)) {
                script = scriptFactory.newInstance(fpo.getPreScript(), ctx);
            }
            if (Objects.nonNull(script)) {
                preScriptCache.put(fpo.getName(), script);
                return script.eval(result);
            }
        } catch (Exception e) {
            log.error(String.format("字段 [%s] 执行前置脚本 [%s] 出现异常", fpo.getName(), fpo.getPreScript().getContent()), e);
            throw new DataGeneratorException(e);
        }
        //转换为数据集再选取一个?
        return result;
    }

    private Object postScript(FieldPO fpo, Object result) {
        try {
            var script = postScriptCache.get(fpo.getName());
            if (Objects.isNull(script)) {
                script = scriptFactory.newInstance(fpo.getPostScript(), ctx);
            }
            if (Objects.nonNull(script)) {
                postScriptCache.put(fpo.getName(), script);
                return script.eval(result);
            }
        } catch (Exception e) {
            log.error(String.format("字段 [%s] 执行后置脚本 [%s] 出现异常", fpo.getName(), fpo.getPostScript().getContent()), e);
            throw new DataGeneratorException(e);
        }
        return result;
    }

    private Object resultMapping(ResultMapperPO rmpo, Object result) {
        if (Objects.isNull(rmpo) || CollectKit.isEmpty(rmpo.getDefaultDataset()) || Objects.isNull(result)) {
            return result;
        }
        var dsm = CollectKit.toMap(rmpo.getMappers(), MapperPO::getKey, MapperPO::getDataset);
        var ds = dsm.get(result.toString());
        return CollectKit.isEmpty(ds) ? RandomKit.choiceOne(rmpo.getDefaultDataset()) : RandomKit.choiceOne(ds);
    }

    private Object convert(FieldPO field, Object value) {
        if (Objects.isNull(field.getConverter())) {
            return Objects.requireNonNull(converterFactory.getBean(StringConverter.class)).convert(value);
        } else {
            return Objects.requireNonNull(converterFactory.getBean(field.getConverter())).convert(value);
        }
    }

    @Override
    public void destroy() {
        this.ctx.global().clear();
        this.fieldDataset.clear();
        this.preScriptCache.values().forEach(script -> {
            try {
                script.close();
            } catch (Exception e) {
                throw new DataGeneratorException(e);
            }
        });
        this.postScriptCache.values().forEach(script -> {
            try {
                script.close();
            } catch (Exception e) {
                throw new DataGeneratorException(e);
            }
        });
        this.preScriptCache.clear();
        this.postScriptCache.clear();
        //set null
        this.converterFactory = null;
        this.scriptFactory = null;
        this.ctx = null;
        this.fieldDataset = null;
        this.preScriptCache = null;
        this.postScriptCache = null;
    }
}
