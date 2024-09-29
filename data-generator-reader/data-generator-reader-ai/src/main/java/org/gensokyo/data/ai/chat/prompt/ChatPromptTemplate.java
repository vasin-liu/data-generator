/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat.prompt;

import org.gensokyo.data.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 会话提示词模板
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/27 , Version 1.0.0
 */
public class ChatPromptTemplate implements PromptTemplateActions, PromptTemplateChatActions {

    private final List<PromptTemplate> promptTemplates;

    public ChatPromptTemplate(List<PromptTemplate> promptTemplates) {
        this.promptTemplates = promptTemplates;
    }

    @Override
    public String render() {
        StringBuilder sb = new StringBuilder();
        for (PromptTemplate promptTemplate : promptTemplates) {
            sb.append(promptTemplate.render());
        }
        return sb.toString();
    }

    @Override
    public String render(Map<String, Object> model) {
        StringBuilder sb = new StringBuilder();
        for (PromptTemplate promptTemplate : promptTemplates) {
            sb.append(promptTemplate.render(model));
        }
        return sb.toString();
    }

    @Override
    public List<Message> createMessages() {
        List<Message> messages = new ArrayList<>();
        for (PromptTemplate promptTemplate : promptTemplates) {
            messages.add(promptTemplate.createMessage());
        }
        return messages;
    }

    @Override
    public List<Message> createMessages(Map<String, Object> model) {
        List<Message> messages = new ArrayList<>();
        for (PromptTemplate promptTemplate : promptTemplates) {
            messages.add(promptTemplate.createMessage(model));
        }
        return messages;
    }

    @Override
    public Prompt create() {
        List<Message> messages = createMessages();
        return new Prompt(messages);
    }

    @Override
    public Prompt create(Map<String, Object> model) {
        List<Message> messages = createMessages(model);
        return new Prompt(messages);
    }

}

