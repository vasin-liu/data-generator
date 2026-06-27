/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.api.console.dto.AuditEventView;
import org.gensokyo.data.audit.AuditService;
import org.gensokyo.data.audit.DatasourceAuditActions;
import org.gensokyo.data.model.vo.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only operator audit log for the console.
 *
 * @author Gensokyo
 * @since 2026-06-07
 */
@RestController
@RequestMapping("/api/console/audit")
@RequiredArgsConstructor
public class ConsoleAuditController {

    private final AuditService auditService;

    /**
     * Lists recent audit events with optional filters.
     *
     * @param action       optional action code filter (e.g. TEMPLATE_PUBLISH)
     * @param resourceType optional resource type filter
     * @param category     optional category alias ({@code DATASOURCE} maps to datasource events, D-25)
     * @param limit        max rows (default 100, max 500)
     * @return sanitized audit rows newest first
     */
    @GetMapping
    public R<List<AuditEventView>> list(
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "resourceType", required = false) String resourceType,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        String resolvedResourceType = resourceType;
        if (category != null && !category.isBlank()
                && DatasourceAuditActions.CATEGORY.equalsIgnoreCase(category.trim())) {
            resolvedResourceType = DatasourceAuditActions.CATEGORY;
        }
        return R.ok(auditService.listRecent(action, resolvedResourceType, limit));
    }
}
