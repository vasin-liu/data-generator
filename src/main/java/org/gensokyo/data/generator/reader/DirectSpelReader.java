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
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.Objects;

/**
 * Spring 表达式引擎读取器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/6 , Version 1.0.0
 */
@Slf4j
public class DirectSpelReader extends AbstractReader {
    private final SpelExpressionParser parser;
    private final StandardEvaluationContext sec;

    public DirectSpelReader(final ReaderPO rpo, final ScriptFactory scriptFactory, final DataFaker dataFaker) {
        super(Objects.requireNonNull(rpo), Objects.requireNonNull(scriptFactory));
        this.parser = new SpelExpressionParser();
        this.sec = new StandardEvaluationContext();
        this.sec.setVariable(Const.SCRIPT_VAR_FAKER, Objects.requireNonNull(dataFaker));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Dataset read(final Context ctx) {
        final String rightBrace1 = "{";
        final String rightBrace2 = "#{";
        final String leftBrace = "}";
        if (rpo.getDataSet() instanceof String dataset) {
            try {
                this.sec.setVariable(Const.SCRIPT_VAR_CTX, ctx);
                if (!dataset.startsWith(rightBrace1) && !dataset.startsWith(rightBrace2)) {
                    dataset = rightBrace1.concat(dataset);
                }
                if (!dataset.endsWith(leftBrace)) {
                    dataset = dataset.concat(leftBrace);
                }
                final String script = dataset;
                return ReadableDataset.of(() -> evalScript(ctx, parser.parseExpression(script).getValue(sec, List.class)));
            } catch (Exception e) {
                log.error("Reader [{}] 在执行表达式 [{}] 出现异常：", rpo.getDataSetId(), dataset);
                throw new DataGeneratorException("执行表达式出现异常", e);
            }
        }
        return ReadableDataset.empty();
    }
}
