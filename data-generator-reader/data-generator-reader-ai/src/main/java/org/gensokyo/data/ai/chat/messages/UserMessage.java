/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat.messages;

import org.springframework.core.io.Resource;

import java.util.List;

/**
 * 用户消息类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
public class UserMessage extends AbstractMessage {

    public UserMessage(String message) {
        super(MessageType.USER, message);
    }

    public UserMessage(Resource resource) {
        super(MessageType.USER, resource);
    }

    public UserMessage(String textContent, List<Media> mediaList) {
        super(MessageType.USER, textContent, mediaList);
    }

    @Override
    public String toString() {
        return "UserMessage{" + "content='" + getContent() + '\'' + ", properties=" + properties + ", messageType="
                + messageType + '}';
    }

}
