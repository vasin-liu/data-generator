/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.cache.DataCache;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.po.condition.WhenPO;
import org.gensokyo.data.po.stage.ConditionStagePO;
import org.gensokyo.data.po.stage.ScriptStagePO;
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
public class ConditionStage extends AbstractStage<ConditionStagePO> {
    private ScriptFactory scriptFactory;

    @Autowired
    public void setScriptFactory(ScriptFactory scriptFactory) {
        this.scriptFactory = scriptFactory;
    }

    public ConditionStage(StageContext<ConditionStagePO> ctx) {
        super(ctx);
    }

    @Override
    public Value internalExecute(Value input) {
        var cpo = ctx.stage();
        var ds = DataCache.getOrCreate(ctx.template().getName());
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
            throw new DataGeneratorException(String.format("字段 %s 的执行条件阶段失败，输入值为：%s。",
                    ctx.field().getName(), JsonKit.write(input.get())), e);
        }
    }

    private Pair<Boolean, Value> choose(ConditionStagePO cpo, Value input, DataCache.TableDataCache ds) {
        if (CollectKit.isEmpty(cpo.getChoose())) {
            return Pair.of(false, input);
        }
        for (WhenPO when : cpo.getChoose()) {
            var result = when(when, input, ds);
            if (Boolean.TRUE.equals(result.getLeft())) {
                return result;
            }
        }
        return Pair.of(false, input);
    }

    private Pair<Boolean, Value> when(WhenPO when, Value input, DataCache.TableDataCache ds) {
        var wssp = new ScriptStagePO(when.getWhen().getScriptType(), when.getWhen().getContent());
        var whenScript = scriptFactory.newInstance(wssp);
        if (Objects.isNull(whenScript)) {
            return Pair.of(false, input);
        }

        Value val = whenScript.eval(wssp, input, ds);
        if (!(val.get() instanceof Boolean flag)) {
            throw new DataGeneratorException(String.format("字段 %s 的执行条件阶段失败，条件表达式 %s 的执行结果不为 Boolean 值。",
                    ctx.field().getName(), when.getWhen().getContent()));
        }

        if (Boolean.TRUE.equals(flag)) {
            var tssp = new ScriptStagePO(when.getThen().getScriptType(), when.getThen().getContent());
            var thenScript = scriptFactory.newInstance(tssp);
            if (Objects.nonNull(thenScript)) {
                return Pair.of(true, thenScript.eval(tssp, input, ds));
            }
        }

        return Pair.of(false, input);
    }

    private Pair<Boolean, Value> otherwise(ConditionStagePO cpo, Value input, DataCache.TableDataCache ds) {
        if (Objects.nonNull(cpo.getOtherwise())) {
            var ossp = new ScriptStagePO(cpo.getOtherwise().getThen().getScriptType(), cpo.getOtherwise().getThen().getContent());
            var otherwiseScript = scriptFactory.newInstance(ossp);
            if (Objects.nonNull(otherwiseScript)) {
                return Pair.of(true, otherwiseScript.eval(ossp, input, ds));
            }
        }
        return Pair.of(false, input);
    }
}
