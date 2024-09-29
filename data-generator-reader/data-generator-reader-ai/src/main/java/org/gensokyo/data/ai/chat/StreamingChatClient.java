/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat;

import org.gensokyo.data.ai.chat.prompt.Prompt;
import org.gensokyo.data.ai.model.StreamingModelClient;
import reactor.core.publisher.Flux;

/**
 * 会话流客户端接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
@FunctionalInterface
public interface StreamingChatClient extends StreamingModelClient<Prompt, ChatResponse> {

    default Flux<String> stream(String message) {
        Prompt prompt = new Prompt(message);
        return stream(prompt).map(response -> (response.getResult() == null || response.getResult().getOutput() == null
                || response.getResult().getOutput().getContent() == null) ? ""
                : response.getResult().getOutput().getContent());
    }

    @Override
    Flux<ChatResponse> stream(Prompt prompt);

}
