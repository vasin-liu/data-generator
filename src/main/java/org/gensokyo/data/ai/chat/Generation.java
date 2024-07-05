/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat;

import org.gensokyo.data.ai.chat.messages.AssistantMessage;
import org.gensokyo.data.ai.chat.metadata.ChatGenerationMetadata;
import org.gensokyo.data.ai.model.ModelResult;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * 生成结果
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
public class Generation implements ModelResult<AssistantMessage> {

    private AssistantMessage assistantMessage;

    private ChatGenerationMetadata chatGenerationMetadata;

    public Generation(String text) {
        this.assistantMessage = new AssistantMessage(text);
    }

    public Generation(String text, Map<String, Object> properties) {
        this.assistantMessage = new AssistantMessage(text, properties);
    }

    @Override
    public AssistantMessage getOutput() {
        return this.assistantMessage;
    }

    @Override
    public ChatGenerationMetadata getMetadata() {
        ChatGenerationMetadata chatGenerationMetadata = this.chatGenerationMetadata;
        return chatGenerationMetadata != null ? chatGenerationMetadata : ChatGenerationMetadata.NULL;
    }

    public Generation withGenerationMetadata(@Nullable ChatGenerationMetadata chatGenerationMetadata) {
        this.chatGenerationMetadata = chatGenerationMetadata;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Generation that)) {
            return false;
        }
        return Objects.equals(assistantMessage, that.assistantMessage)
                && Objects.equals(chatGenerationMetadata, that.chatGenerationMetadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assistantMessage, chatGenerationMetadata);
    }

    @Override
    public String toString() {
        return "Generation{" + "assistantMessage=" + assistantMessage + ", chatGenerationMetadata="
                + chatGenerationMetadata + '}';
    }

}