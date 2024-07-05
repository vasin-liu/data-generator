/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.parser;

import org.springframework.messaging.converter.MessageConverter;

/**
 * 消息转换解析抽象类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/5/11 , Version 1.0.0
 */
public abstract class AbstractMessageConverterOutputParser<T> implements OutputParser<T> {

    private MessageConverter messageConverter;

    public AbstractMessageConverterOutputParser(MessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    public MessageConverter getMessageConverter() {
        return this.messageConverter;
    }

}
