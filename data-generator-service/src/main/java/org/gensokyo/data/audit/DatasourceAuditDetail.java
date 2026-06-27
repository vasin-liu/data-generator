/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.audit;

import org.gensokyo.data.datasource.api.ConnectionKind;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds summary-only audit detail maps for datasource events (D-23).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
public final class DatasourceAuditDetail {

    private DatasourceAuditDetail() {
    }

    /**
     * @param connectionName catalog entry name
     * @param kind           connection kind
     * @param action         audit action code
     * @return summary detail without secrets or full JDBC URLs
     */
    public static Map<String, Object> summary(String connectionName, ConnectionKind kind, String action) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("connectionName", connectionName);
        detail.put("kind", kind.name());
        detail.put("action", action);
        return Map.copyOf(detail);
    }

    /**
     * @param connectionName catalog entry name
     * @param kind           connection kind
     * @param action         audit action code
     * @param outcome        reload/test outcome (e.g. success, failure)
     * @param reason         optional operator-facing reason (truncated, no secrets)
     * @return summary detail
     */
    public static Map<String, Object> summary(
            String connectionName,
            ConnectionKind kind,
            String action,
            String outcome,
            String reason) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("connectionName", connectionName);
        detail.put("kind", kind.name());
        detail.put("action", action);
        if (outcome != null && !outcome.isBlank()) {
            detail.put("outcome", outcome);
        }
        if (reason != null && !reason.isBlank()) {
            detail.put("reason", truncate(reason));
        }
        return Map.copyOf(detail);
    }

    private static String truncate(String reason) {
        return reason.length() > 256 ? reason.substring(0, 256) : reason;
    }
}
