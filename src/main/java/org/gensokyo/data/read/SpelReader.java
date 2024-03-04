/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.read;

import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.constant.Const;
import org.gensokyo.data.exception.DataGeneratorException;
import org.gensokyo.data.faker.DataFaker;
import org.gensokyo.data.po.ReadStagePO;
import org.gensokyo.data.value.ListValue;
import org.gensokyo.data.value.Value;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spring 表达式引擎读取器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/26 , Version 1.0.0
 */
@Slf4j
public class SpelReader extends AbstractReader implements InitializingBean {
    private final SpelExpressionParser parser;
    private final StandardEvaluationContext sec;
    private final Pattern p = Pattern.compile("^\\{.+\\}$|^((?!\\{).+(?!\\}))\\[(\\d+)\\]$");

    private DataFaker dataFaker;

    @Autowired
    public void setDataFaker(DataFaker dataFaker) {
        this.dataFaker = dataFaker;
    }

    protected SpelReader(ReadStagePO.ReaderPO rpo) {
        super(rpo);
        this.parser = new SpelExpressionParser();
        this.sec = new StandardEvaluationContext();
        this.sec.addPropertyAccessor(new MapAccessor());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Value read(final Value input) {
        if (rpo.getDataSet() instanceof String dataset) {
            try {
                Matcher m = p.matcher(dataset);
                if (m.find()) {
                    var num = m.group(2);
                    if (Objects.isNull(num)) {
                        //原生SPEL表达式
                        var el = m.group(0);
                        log.debug("当前为原生SPEL表达式模式，表达式内容为：{}", el);
                        var evalResult = parser.parseExpression(el).getValue(sec, List.class);
                        return ListValue.fromObjectList(evalResult);
                    } else {
                        //自定义SPEL表达式
                        var list = new ArrayList<>();
                        var el = m.group(1);
                        log.debug("当前为自定义SPEL表达式模式，表达式内容为：{}，执行次数为：{}", el, num);
                        for (int i = 0; i < Integer.parseInt(num); i++) {
                            list.add(parser.parseExpression(el).getValue(sec));
                        }
                        return ListValue.fromObjectList(list);
                    }
                }
                log.error("Reader [{}] 不支持表达式 [{}] ", rpo.getDataSetId(), dataset);
                throw new DataGeneratorException(String.format("Reader [%s] 不支持表达式 [%s] ", rpo.getDataSetId(), dataset));
            } catch (Exception e) {
                log.error("Reader [{}] 在执行表达式 [{}] 出现异常", rpo.getDataSetId(), dataset);
                throw new DataGeneratorException("执行表达式出现异常", e);
            }
        }
        return Value.EMPTY;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.sec.setVariable(Const.SCRIPT_VAR_FAKER, Objects.requireNonNull(dataFaker));
    }
}
