/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.gensokyo.data.ai.chat.ChatResponse;
import org.gensokyo.data.ai.chat.prompt.Prompt;
import org.gensokyo.data.ai.parser.OutputParser;
import org.gensokyo.data.context.StageContext;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.util.DatasetKit;
import org.gensokyo.data.value.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * AI数据读取器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/1 , Version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class AiReader<S extends ReadStageVO, T extends AiReaderVO> implements Reader<S, T> {
    private final ApplicationContext ctx;

    @Setter(onMethod_ = @Autowired)
    private ChatClientFactory chatClientFactory;

    @Override
    public Value read(StageContext<S> sctx, final T rvo, final Value input) {
        var client = chatClientFactory.newInstance(rvo, rvo.getProvider());
        OutputParser<?> parser = ctx.getBean(rvo.getParser());
        Prompt prompt = new Prompt(rvo.getPrompt());
        ChatResponse resp = client.call(prompt);
        log.info("AI生成结果：{}", resp);
        //TODO 提取停止符之间的内容
        Object parsed = parser.parse(resp.getResult().getOutput().getContent());
        return DatasetKit.toValue(parsed);
    }
}
