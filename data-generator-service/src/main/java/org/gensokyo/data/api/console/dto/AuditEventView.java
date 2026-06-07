/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Console-facing audit row (detail is sanitized; never includes secrets).
 *
 * @param id           event id
 * @param occurredAt   timestamp
 * @param actor        operator identity
 * @param action       action code
 * @param resourceType resource category
 * @param resourceId   resource identifier
 * @param detail       optional structured detail
 * @author Gensokyo
 * @since 2026-06-07
 */
public record AuditEventView(
        Long id,
        Instant occurredAt,
        String actor,
        String action,
        String resourceType,
        String resourceId,
        Map<String, Object> detail) {
}
