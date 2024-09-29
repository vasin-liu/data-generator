/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.model;

import reactor.core.publisher.Flux;

/**
 * 模型流式客户端接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
public interface StreamingModelClient<I extends ModelRequest<?>, O extends ModelResponse<?>> {

    Flux<O> stream(I request);
}
