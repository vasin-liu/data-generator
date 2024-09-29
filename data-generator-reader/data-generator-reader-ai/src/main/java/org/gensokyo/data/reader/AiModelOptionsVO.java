/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.ai.ollama.api.OllamaOptions;

import java.io.Serializable;

/**
 * AI模型配置类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/24 , Version 1.0.0
 */
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME
        , include = JsonTypeInfo.As.EXISTING_PROPERTY
        , property = "type"
        , visible = true
        // 反序列化时，如果没有匹配到子类，则使用默认实现类
        , defaultImpl = OllamaOptions.class
)
public class AiModelOptionsVO implements Serializable {

    private AiProvider type = AiProvider.OLLAMA;
}
