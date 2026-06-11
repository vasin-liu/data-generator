/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat.messages;

import java.util.Map;

/**
 * 助手消息类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
public class AssistantMessage extends AbstractMessage {

    public AssistantMessage(String content) {
        super(MessageType.ASSISTANT, content);
    }

    public AssistantMessage(String content, Map<String, Object> properties) {
        super(MessageType.ASSISTANT, content, properties);
    }

    @Override
    public String toString() {
        return "AssistantMessage{" + "content='" + getContent() + '\'' + ", properties=" + properties + ", messageType="
                + messageType + '}';
    }

}
