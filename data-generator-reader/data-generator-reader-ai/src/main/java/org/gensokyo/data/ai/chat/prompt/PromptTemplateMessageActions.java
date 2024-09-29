/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat.prompt;

import org.gensokyo.data.ai.chat.messages.Media;
import org.gensokyo.data.ai.chat.messages.Message;

import java.util.List;
import java.util.Map;

/**
 * 提示词模糊消息动作
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/27 , Version 1.0.0
 */
public interface PromptTemplateMessageActions {

    Message createMessage();

    Message createMessage(List<Media> mediaList);

    Message createMessage(Map<String, Object> model);

}