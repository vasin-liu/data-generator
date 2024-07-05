/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat.messages;

import java.util.Map;

/**
 * 会话消息
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
public class ChatMessage extends AbstractMessage {

    public ChatMessage(String role, String content) {
        super(MessageType.valueOf(role), content);
    }

    public ChatMessage(String role, String content, Map<String, Object> properties) {
        super(MessageType.valueOf(role), content, properties);
    }

    public ChatMessage(MessageType messageType, String content) {
        super(messageType, content);
    }

    public ChatMessage(MessageType messageType, String content, Map<String, Object> properties) {
        super(messageType, content, properties);
    }

}
