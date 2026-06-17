/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.model.po;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite primary key for scoped AI quota counters ({@code usageDate} + {@code scopeKey}).
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class AiQuotaScopeDailyUsageId implements Serializable {

    @Column(name = "usage_date", length = 10, nullable = false)
    private String usageDate;

    @Column(name = "scope_key", length = 256, nullable = false)
    private String scopeKey;
}
