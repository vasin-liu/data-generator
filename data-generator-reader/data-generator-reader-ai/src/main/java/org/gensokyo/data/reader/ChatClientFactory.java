/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader;

import org.gensokyo.data.ai.chat.ChatClient;
import org.gensokyo.data.ai.ollama.OllamaChatClient;
import org.gensokyo.data.ai.ollama.api.OllamaApi;
import org.gensokyo.data.ai.ollama.api.OllamaOptions;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * 会话客户端工厂类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/26 , Version 1.0.0
 */
public class ChatClientFactory {

    public @NonNull <R extends AiReaderVO, O extends AiModelOptionsVO, T extends AiProviderVO<O>> ChatClient newInstance(
            final R rpo, final T pvo) {
        ChatClient client = switch (pvo.getType()) {
            case OLLAMA -> new OllamaChatClient(new OllamaApi(rpo.getApi()), (OllamaOptions) pvo.getOptions());
        };

        Assert.notNull(client, "未找到类型为 " + pvo.getType() + " 的AI客户端类");
        return client;
    }
}