/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.cache.DataSet;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.condition.WhenVO;
import org.gensokyo.data.model.vo.stage.ScriptStageVO;
import org.gensokyo.data.script.ScriptFactory;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.collect.CollectKit;
import org.gensokyo.kit.json.JsonKit;
import org.gensokyo.kit.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 * 条件分支阶段
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/27 , Version 1.0.0
 */
@Slf4j
public class ConditionStage extends AbstractStage<ConditionStageVO> {
    private ScriptFactory scriptFactory;

    @Autowired
    public void setScriptFactory(ScriptFactory scriptFactory) {
        this.scriptFactory = scriptFactory;
    }

    public ConditionStage(StageContext<ConditionStageVO> ctx) {
        super(ctx);
    }

    @Override
    public Value internalExecute(Value input) {
        var cpo = ctx.stage();
        var ds = DataSet.getOrCreate(ctx.template().getId(), ctx.template().getInstanceId());
        try {
            var choose = choose(cpo, input, ds);
            if (Boolean.TRUE.equals(choose.getLeft())) {
                return choose.getRight();
            }

            //其他
            var otherwise = otherwise(cpo, input, ds);
            if (Boolean.TRUE.equals(otherwise.getLeft())) {
                return otherwise.getRight();
            }

            // 无任何条件匹配
            return input;
        } catch (Exception e) {
            var msg = String.format("执行条件判断阶段失败，输入值为：%s，上下文信息为：%s", input, JsonKit.write(ctx));
            throw new DataGeneratorException(msg, e);
        }
    }

    private Pair<Boolean, Value> choose(ConditionStageVO cpo, Value input, DataSet.TableDataSet ds) {
        if (CollectKit.isEmpty(cpo.getChoose())) {
            return Pair.of(false, input);
        }
        for (var when : cpo.getChoose()) {
            var result = when(when, input, ds);
            if (Boolean.TRUE.equals(result.getLeft())) {
                return result;
            }
        }
        return Pair.of(false, input);
    }

    private Pair<Boolean, Value> when(WhenVO when, Value input, DataSet.TableDataSet ds) {
        var wssp = new ScriptStageVO(when.getWhen());
        var whenScript = scriptFactory.newInstance(wssp.getLanguage());
        if (Objects.isNull(whenScript)) {
            return Pair.of(false, input);
        }

        var wctx = new StageContext<>(ctx.template(), ctx.field(), wssp);
        var val = whenScript.eval(wctx, wssp.getLanguage(), input, ds);
        if (!(val.get() instanceof Boolean flag)) {
            var msg = String.format("执行条件判断阶段失败，条件表达式 %s 的执行结果不为 Boolean 值，输入值为：%s，上下文信息为：%s",
                    when.getWhen().getContent(), input, JsonKit.write(ctx));
            throw new DataGeneratorException(msg);
        }

        if (Boolean.TRUE.equals(flag)) {
            var tssp = new ScriptStageVO(when.getThen());
            var thenScript = scriptFactory.newInstance(tssp.getLanguage());
            if (Objects.isNull(thenScript)) {
                return Pair.of(false, input);
            }
            var tctx = new StageContext<>(ctx.template(), ctx.field(), tssp);
            return Pair.of(true, thenScript.eval(tctx, tssp.getLanguage(), input, ds));
        }

        return Pair.of(false, input);
    }

    private Pair<Boolean, Value> otherwise(ConditionStageVO cpo, Value input, DataSet.TableDataSet ds) {
        if (Objects.nonNull(cpo.getOtherwise())) {
            var ossp = new ScriptStageVO(cpo.getOtherwise().getThen());
            var otherwiseScript = scriptFactory.newInstance(ossp.getLanguage());
            if (Objects.nonNull(otherwiseScript)) {
                var octx = new StageContext<>(ctx.template(), ctx.field(), ossp);
                return Pair.of(true, otherwiseScript.eval(octx, ossp.getLanguage(), input, ds));
            }
        }
        return Pair.of(false, input);
    }
}
