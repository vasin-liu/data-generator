/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.usage;

/**
 * Thrown when a platform AI daily quota would be exceeded.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
public class AiQuotaExceededException extends IllegalStateException {

    /**
     * @param message operator-facing quota violation detail
     */
    public AiQuotaExceededException(String message) {
        super(message);
    }
}
