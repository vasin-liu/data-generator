/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Controls how Template V2 sink writers behave on failure.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
@Getter
@Setter
public class SinkExecutionPolicyVO implements Serializable {
    /** Failure handling mode, e.g. {@code FAIL_FAST} or {@code CONTINUE_ON_ERROR}. */
    private String mode;
    /** Maximum number of attempts per sink write, including the first try. */
    private Integer maxRetries;
    /** Backoff in milliseconds between retry attempts. */
    private Integer retryBackoffMs;
}
