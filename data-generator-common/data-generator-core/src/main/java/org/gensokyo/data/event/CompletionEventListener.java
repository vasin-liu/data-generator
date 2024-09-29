/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.event;

import org.gensokyo.data.value.Value;

/**
 * 完成事件监听器
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/16 , Version 1.0.0
 */
public interface CompletionEventListener extends EventListener {

    @Override
    default boolean support(Event event) {
        return event.getSource() instanceof Value;
    }
}
