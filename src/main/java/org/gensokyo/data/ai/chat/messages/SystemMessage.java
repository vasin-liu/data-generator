/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat.messages;

import org.springframework.core.io.Resource;

/**
 * 系统消息
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
public class SystemMessage extends AbstractMessage {

    public SystemMessage(String content) {
        super(MessageType.SYSTEM, content);
    }

    public SystemMessage(Resource resource) {
        super(MessageType.SYSTEM, resource);
    }

    @Override
    public String toString() {
        return "SystemMessage{" + "content='" + getContent() + '\'' + ", properties=" + properties + ", messageType="
                + messageType + '}';
    }

}
