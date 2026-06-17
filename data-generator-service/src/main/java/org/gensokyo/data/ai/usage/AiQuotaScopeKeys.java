/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.usage;

import java.util.Locale;

/**
 * Canonical scope key builders for AI quota buckets.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
public final class AiQuotaScopeKeys {

    /** Platform-wide quota bucket stored in {@code ai_quota_daily_usage}. */
    public static final String PLATFORM = "platform";

    private AiQuotaScopeKeys() {
    }

    /**
     * @param providerType provider identifier
     * @return scoped key {@code provider:<TYPE>}
     */
    public static String provider(String providerType) {
        return "provider:" + normalize(providerType);
    }

    /**
     * @param templateId template snowflake id
     * @return scoped key {@code template:<id>}
     */
    public static String template(String templateId) {
        return "template:" + templateId.trim();
    }

    /**
     * @param scopeType configured scope type
     * @param scopeKey  configured scope key
     * @return canonical scoped bucket key
     */
    public static String configured(String scopeType, String scopeKey) {
        if (scopeType == null || scopeType.isBlank()) {
            throw new IllegalArgumentException("AI quota scope type must not be blank");
        }
        String normalizedType = scopeType.trim().toUpperCase(Locale.ROOT);
        if ("PROVIDER".equals(normalizedType)) {
            return provider(scopeKey);
        }
        if ("TEMPLATE".equals(normalizedType)) {
            return template(scopeKey);
        }
        throw new IllegalArgumentException("Unsupported AI quota scope type [" + scopeType + "]");
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("AI quota scope key must not be blank");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
