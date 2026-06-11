/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat.metadata;

import org.gensokyo.data.ai.model.ResponseMetadata;

/**
 * 会话响应元数据接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
public interface ChatResponseMetadata extends ResponseMetadata {

    ChatResponseMetadata NULL = new ChatResponseMetadata() {
    };

    default RateLimit getRateLimit() {
        return new EmptyRateLimit();
    }

    default Usage getUsage() {
        return new EmptyUsage();
    }

    default PromptMetadata getPromptMetadata() {
        return PromptMetadata.empty();
    }

}
