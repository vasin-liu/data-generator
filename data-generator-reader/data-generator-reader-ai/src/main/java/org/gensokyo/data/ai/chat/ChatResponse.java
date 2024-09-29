/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat;

import org.gensokyo.data.ai.chat.metadata.ChatResponseMetadata;
import org.gensokyo.data.ai.model.ModelResponse;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * 会话响应接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
public class ChatResponse implements ModelResponse<Generation> {

    private final ChatResponseMetadata chatResponseMetadata;

    private final List<Generation> generations;

    public ChatResponse(List<Generation> generations) {
        this(generations, ChatResponseMetadata.NULL);
    }

    public ChatResponse(List<Generation> generations, ChatResponseMetadata chatResponseMetadata) {
        this.chatResponseMetadata = chatResponseMetadata;
        this.generations = List.copyOf(generations);
    }

    @Override
    public List<Generation> getResults() {
        return this.generations;
    }

    @Override
    public Generation getResult() {
        if (CollectionUtils.isEmpty(this.generations)) {
            return null;
        }
        return this.generations.get(0);
    }

    @Override
    public ChatResponseMetadata getMetadata() {
        return this.chatResponseMetadata;
    }

    @Override
    public String toString() {
        return "ChatResponse [metadata=" + chatResponseMetadata + ", generations=" + generations + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChatResponse that)) {
            return false;
        }
        return Objects.equals(chatResponseMetadata, that.chatResponseMetadata)
                && Objects.equals(generations, that.generations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatResponseMetadata, generations);
    }

}
