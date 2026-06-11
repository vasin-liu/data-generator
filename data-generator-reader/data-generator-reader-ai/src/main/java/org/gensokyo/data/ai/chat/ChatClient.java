/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat;

import org.gensokyo.data.ai.chat.messages.UserMessage;
import org.gensokyo.data.ai.chat.prompt.Prompt;
import org.gensokyo.data.ai.model.ModelClient;

/**
 * 会话客户端接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
@FunctionalInterface
public interface ChatClient extends ModelClient<Prompt, ChatResponse> {

    default String call(String message) {
        Prompt prompt = new Prompt(new UserMessage(message));
        Generation generation = call(prompt).getResult();
        return (generation != null) ? generation.getOutput().getContent() : "";
    }

    @Override
    ChatResponse call(Prompt prompt);

}
