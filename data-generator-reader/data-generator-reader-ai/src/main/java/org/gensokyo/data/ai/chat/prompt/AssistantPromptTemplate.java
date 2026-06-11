/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat.prompt;

import org.gensokyo.data.ai.chat.messages.AssistantMessage;
import org.gensokyo.data.ai.chat.messages.Message;
import org.springframework.core.io.Resource;

import java.util.Map;

/**
 * 助手提示词模板
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/27 , Version 1.0.0
 */
public class AssistantPromptTemplate extends PromptTemplate {

    public AssistantPromptTemplate(String template) {
        super(template);
    }

    public AssistantPromptTemplate(Resource resource) {
        super(resource);
    }

    @Override
    public Prompt create() {
        return new Prompt(new AssistantMessage(render()));
    }

    @Override
    public Prompt create(Map<String, Object> model) {
        return new Prompt(new AssistantMessage(render(model)));
    }

    @Override
    public Message createMessage() {
        return new AssistantMessage(render());
    }

    @Override
    public Message createMessage(Map<String, Object> model) {
        return new AssistantMessage(render(model));
    }

}
