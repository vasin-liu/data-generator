/*
 * Copyright © 2025 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.stage;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.cache.DataSet;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.model.vo.stage.ScriptStageVO;
import org.gensokyo.data.script.ScriptFactory;
import org.gensokyo.data.value.Value;
import org.gensokyo.kit.Assert;
import org.gensokyo.kit.json.JsonKit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.NumberUtils;

import java.time.Duration;
import java.util.Objects;

/**
 * 暂停阶段
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2025/4/6 , Version 1.0.0
 */
@Slf4j
public class PauseStage extends AbstractStage<PauseStageVO> {
    @Setter(onMethod_ = @Autowired)
    private ScriptFactory scriptFactory;

    public PauseStage(StageContext<PauseStageVO> ctx) {
        super(ctx);
    }

    @Override
    public Value internalExecute(Value input) {
        Assert.isTrue(Objects.nonNull(ctx.stage().getDuration()) || Objects.nonNull(ctx.stage().getLanguage()),
                "暂停时长[duration]和脚本配置[language]不能同时为空");
        //暂停当前线程
        int amount = duration(input);
        Assert.isTrue(amount > 0, "暂停时间结果值必须大于0");
        var duration = Duration.of(amount, Objects.requireNonNull(ctx.stage().getUnit(), "暂停时间单位值不能为空"));
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            var msg = String.format("执行流水线暂停阶段失败，输入值为：%s，上下文信息为：%s", input, JsonKit.write(ctx));
            throw new DataGeneratorException(msg, e);
        }
        return input;
    }

    private Integer duration(Value input) {
        var psvo = ctx.stage();
        if (Objects.nonNull(psvo.getDuration()) && psvo.getDuration() > 0) {
            return psvo.getDuration();
        }
        var sspo = new ScriptStageVO();
        sspo.setType(Const.StageType.SCRIPT);
        sspo.setLanguage(psvo.getLanguage());
        var sctx = new StageContext<>(ctx.template(), null, sspo);
        var spo = sctx.stage().getLanguage();
        var script = scriptFactory.newInstance(spo);
        var args = DataSet.getOrCreate(ctx.template().getId(), ctx.template().getInstanceId());
        var v = Objects.requireNonNull(script).eval(sctx, spo, input, args).get();
        return NumberUtils.parseNumber(Objects.toString(v), Integer.class);
    }
}
