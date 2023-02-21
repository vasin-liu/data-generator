/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.reader;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.generator.constant.Const;
import org.gensokyo.data.generator.dataset.Dataset;
import org.gensokyo.data.generator.dataset.ReadableDataset;
import org.gensokyo.data.generator.domain.Context;
import org.gensokyo.data.generator.domain.ReaderPO;
import org.gensokyo.data.generator.exception.DataGeneratorException;
import org.gensokyo.data.generator.factory.ScriptFactory;
import org.gensokyo.data.generator.faker.DataFaker;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spring 表达式引擎读取器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/6 , Version 1.0.0
 */
@Slf4j
public class SpelReader extends AbstractReader {
    private final SpelExpressionParser parser;
    private final StandardEvaluationContext sec;
    private final Pattern p = Pattern.compile("^\\{.+\\}$|^((?!\\{).+(?!\\}))\\[(\\d+)\\]$");

    public SpelReader(final ReaderPO rpo, final ScriptFactory scriptFactory, final DataFaker dataFaker) {
        super(Objects.requireNonNull(rpo), Objects.requireNonNull(scriptFactory));
        this.parser = new SpelExpressionParser();
        this.sec = new StandardEvaluationContext();
        this.sec.addPropertyAccessor(new MapAccessor());
        this.sec.setVariable(Const.SCRIPT_VAR_FAKER, Objects.requireNonNull(dataFaker));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Dataset read(final Context ctx) {
        if (rpo.getDataSet() instanceof String dataset) {
            try {
                this.sec.setVariable(Const.SCRIPT_VAR_CTX, ctx);
                Matcher m = p.matcher(dataset);
                if (m.find()) {
                    var num = m.group(2);
                    if (Objects.isNull(num)) {
                        //原生SPEL表达式
                        var el = m.group(0);
                        log.debug("当前为原生SPEL表达式模式，表达式内容为：{}", el);
                        var data = evalScript(ctx, parser.parseExpression(el).getValue(sec, List.class));
                        return ReadableDataset.of(data);
                    } else {
                        //自定义SPEL表达式
                        var list = new ArrayList<>();
                        var el = m.group(1);
                        log.debug("当前为自定义SPEL表达式模式，表达式内容为：{}，执行次数为：{}", el, num);
                        for (int i = 0; i < Integer.parseInt(num); i++) {
                            list.add(parser.parseExpression(el).getValue(sec));
                        }
                        var data = evalScript(ctx, list);
                        return ReadableDataset.of(data);
                    }
                }
                log.error("Reader [{}] 不支持表达式 [{}] ", rpo.getDataSetId(), dataset);
                throw new DataGeneratorException(String.format("Reader [%s] 不支持表达式 [%s] ", rpo.getDataSetId(), dataset));
            } catch (Exception e) {
                log.error("Reader [{}] 在执行表达式 [{}] 出现异常", rpo.getDataSetId(), dataset);
                throw new DataGeneratorException("执行表达式出现异常", e);
            }
        }
        return ReadableDataset.empty();
    }
}
