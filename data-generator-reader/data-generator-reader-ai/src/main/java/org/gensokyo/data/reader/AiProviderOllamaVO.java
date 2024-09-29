/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.reader;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.ai.ollama.api.OllamaOptions;
import org.gensokyo.data.json.JsonSubType;

/**
 * Ollama提供者
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/26 , Version 1.0.0
 */
@Getter
@Setter
@AutoService(AiProviderVO.class)
@JsonSubType(value = "OLLAMA")
public class AiProviderOllamaVO<T extends OllamaOptions> extends AiProviderVO<T> {
}
