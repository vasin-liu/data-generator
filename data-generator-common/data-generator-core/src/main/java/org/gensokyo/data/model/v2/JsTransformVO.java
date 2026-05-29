/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import com.google.auto.service.AutoService;
import lombok.Getter;
import lombok.Setter;
import org.gensokyo.data.json.JsonSubType;

/**
 * Row-local JavaScript transform executed in a sandboxed GraalJS context.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Getter
@Setter
@AutoService(TransformVO.class)
@JsonSubType("JS")
public class JsTransformVO extends TransformVO {

    /**
     * Maximum allowed UTF-8 script size in bytes.
     */
    public static final int MAX_SCRIPT_BYTES = 65_536;

    /**
     * Default script execution timeout in milliseconds.
     */
    public static final int DEFAULT_TIMEOUT_MS = 5_000;

    /**
     * Creates a transform with {@code type} set to {@code js}.
     */
    public JsTransformVO() {
        setType("js");
    }

    /**
     * Inline JavaScript body evaluated once per input row with {@code row} bound to the row map.
     */
    private String script;

    /**
     * Optional per-row execution timeout in milliseconds; defaults to {@link #DEFAULT_TIMEOUT_MS}.
     */
    private Integer timeoutMs;
}
