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
import org.gensokyo.data.value.SingleValue;
import org.gensokyo.data.value.Value;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Spring 表达式引擎读取器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/2/26 , Version 1.0.0
 */
@Slf4j
public class DirectSpelReader extends AbstractReader implements InitializingBean {
    private final SpelExpressionParser parser;
    private final StandardEvaluationContext sec;

    private DataFaker dataFaker;

    @Autowired
    public void setDataFaker(DataFaker dataFaker) {
        this.dataFaker = dataFaker;
    }

    public DirectSpelReader(final ReadStagePO.ReaderPO rpo) {
        super(Objects.requireNonNull(rpo));
        this.parser = new SpelExpressionParser();
        this.sec = new StandardEvaluationContext();
    }

    @Override
    public Value read(final Value input) {
        final String rightBrace1 = "{";
        final String rightBrace2 = "#{";
        final String leftBrace = "}";
        if (rpo.getDataSet() instanceof String dataset) {
            try {
                if (!dataset.startsWith(rightBrace1) && !dataset.startsWith(rightBrace2)) {
                    dataset = rightBrace1.concat(dataset);
                }
                if (!dataset.endsWith(leftBrace)) {
                    dataset = dataset.concat(leftBrace);
                }
                final String script = dataset;
                Supplier<Object> evalResult = () -> parser.parseExpression(script).getValue(sec, List.class);
                return SingleValue.of(evalResult);
            } catch (Exception e) {
                log.error("Reader [{}] 在执行表达式 [{}] 出现异常：", rpo.getDataSetId(), dataset);
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

