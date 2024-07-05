/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat.metadata;

import java.time.Duration;

/**
 * 流量限制空实现
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
public class EmptyRateLimit implements RateLimit {

    @Override
    public Long getRequestsLimit() {
        return 0L;
    }

    @Override
    public Long getRequestsRemaining() {
        return 0L;
    }

    @Override
    public Duration getRequestsReset() {
        return Duration.ZERO;
    }

    @Override
    public Long getTokensLimit() {
        return 0L;
    }

    @Override
    public Long getTokensRemaining() {
        return 0L;
    }

    @Override
    public Duration getTokensReset() {
        return Duration.ZERO;
    }

}
