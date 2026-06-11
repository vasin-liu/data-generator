/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Single transform node in a {@link TransformGraphVO}.
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@Getter
@Setter
@NoArgsConstructor
public class TransformNodeVO implements Serializable {
    private String id;
    /** Id of a transform in {@link TransformGraphVO#getTransforms()}. */
    private String transformId;
    /** Table alias exposed to downstream nodes (for example {@code input}). */
    private String outputAlias;
}
