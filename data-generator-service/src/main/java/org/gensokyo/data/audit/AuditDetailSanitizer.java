/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.audit;

import org.gensokyo.data.json.TemplateJsonCodec;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Redacts sensitive keys from audit detail maps before API exposure.
 *
 * @author Gensokyo
 * @since 2026-06-07
 */
public final class AuditDetailSanitizer {

    private static final Set<String> SENSITIVE_KEY_FRAGMENTS = Set.of("password", "secret", "token", "apikey", "credential");

    private AuditDetailSanitizer() {
    }

    /**
     * @param detailJson stored JSON detail
     * @return sanitized map or empty when absent/invalid
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> sanitizeJson(String detailJson) {
        if (detailJson == null || detailJson.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = TemplateJsonCodec.read(detailJson, Map.class);
            return sanitizeMap(parsed);
        } catch (RuntimeException ignored) {
            return Map.of("raw", "[unparseable detail]");
        }
    }

    private static Map<String, Object> sanitizeMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (isSensitiveKey(entry.getKey())) {
                sanitized.put(entry.getKey(), "[redacted]");
            } else {
                sanitized.put(entry.getKey(), entry.getValue());
            }
        }
        return sanitized;
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        for (String fragment : SENSITIVE_KEY_FRAGMENTS) {
            if (normalized.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
