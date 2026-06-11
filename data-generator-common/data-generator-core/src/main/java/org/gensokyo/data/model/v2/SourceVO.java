/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.v2;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Base Template V2 source configuration shared by iterator, query, and other source subtypes.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
public abstract class SourceVO implements Serializable {
    private String type;
    /** Legacy migration-oriented source policy; prefer {@link #materializationPolicy} for new templates. */
    private SourcePolicyVO policy;
    /** V2-native row materialization and selection policy applied after the source is read. */
    private MaterializationPolicyVO materializationPolicy;
}
