/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat.prompt;

import org.gensokyo.data.ai.chat.messages.Message;
import org.gensokyo.data.ai.chat.messages.UserMessage;
import org.gensokyo.data.ai.model.ModelOptions;
import org.gensokyo.data.ai.model.ModelRequest;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 提示词
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
public class Prompt implements ModelRequest<List<Message>> {

    private final List<Message> messages;

    private ChatOptions modelOptions;

    public Prompt(String contents) {
        this(new UserMessage(contents));
    }

    public Prompt(Message message) {
        this(Collections.singletonList(message));
    }

    public Prompt(List<Message> messages) {
        this.messages = messages;
    }

    public Prompt(String contents, ChatOptions modelOptions) {
        this(new UserMessage(contents), modelOptions);
    }

    public Prompt(Message message, ChatOptions modelOptions) {
        this(Collections.singletonList(message), modelOptions);
    }

    public Prompt(List<Message> messages, ChatOptions modelOptions) {
        this.messages = messages;
        this.modelOptions = modelOptions;
    }

    public String getContents() {
        StringBuilder sb = new StringBuilder();
        for (Message message : getInstructions()) {
            sb.append(message.getContent());
        }
        return sb.toString();
    }

    @Override
    public ModelOptions getOptions() {
        return this.modelOptions;
    }

    @Override
    public List<Message> getInstructions() {
        return this.messages;
    }

    @Override
    public String toString() {
        return "Prompt{" + "messages=" + this.messages + ", modelOptions=" + this.modelOptions + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Prompt prompt)) {
            return false;
        }
        return Objects.equals(this.messages, prompt.messages) && Objects.equals(this.modelOptions, prompt.modelOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.messages, this.modelOptions);
    }

}