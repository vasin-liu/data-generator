/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat.metadata;

import java.time.Duration;

/**
 * 流量限制接口
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
public interface RateLimit {

    Long getRequestsLimit();

    Long getRequestsRemaining();

    Duration getRequestsReset();

    Long getTokensLimit();

    Long getTokensRemaining();

    Duration getTokensReset();

}