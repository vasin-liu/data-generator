/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat.metadata;

import io.micrometer.common.lang.Nullable;
import org.gensokyo.data.ai.model.ResultMetadata;

/**
 * 会话生成元数据接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
public interface ChatGenerationMetadata extends ResultMetadata {

    ChatGenerationMetadata NULL = ChatGenerationMetadata.from(null, null);


    static ChatGenerationMetadata from(String finishReason, Object contentFilterMetadata) {
        return new ChatGenerationMetadata() {

            @Override
            @SuppressWarnings("unchecked")
            public <T> T getContentFilterMetadata() {
                return (T) contentFilterMetadata;
            }

            @Override
            public String getFinishReason() {
                return finishReason;
            }
        };
    }

    @Nullable
    <T> T getContentFilterMetadata();

    String getFinishReason();

}
