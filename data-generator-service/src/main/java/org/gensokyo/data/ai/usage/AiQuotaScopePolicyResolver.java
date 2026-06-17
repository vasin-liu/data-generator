/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.usage;

import org.gensokyo.data.calcite.runtime.AiExecutionScope;
import org.gensokyo.data.config.DataGeneratorProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves configured provider/template scoped AI quota policies.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
final class AiQuotaScopePolicyResolver {

    private AiQuotaScopePolicyResolver() {
    }

    /**
     * @param quota configured quota block
     * @return active scoped policies keyed by canonical scope key
     */
    static Map<String, AiQuotaScopedPolicy> configuredScopes(DataGeneratorProperties.AiRuntimeQuota quota) {
        Map<String, AiQuotaScopedPolicy> scopes = new LinkedHashMap<>();
        if (quota == null || quota.getScopeOverrides() == null) {
            return scopes;
        }
        for (DataGeneratorProperties.AiQuotaScopeOverride override : quota.getScopeOverrides()) {
            if (override == null || override.getScopeType() == null || override.getScopeKey() == null) {
                continue;
            }
            String scopeKey = AiQuotaScopeKeys.configured(override.getScopeType(), override.getScopeKey());
            long maxCalls = override.getMaxCallsPerDay() == null ? 0L : Math.max(0L, override.getMaxCallsPerDay());
            long maxTokens = override.getMaxTokensPerDay() == null ? 0L : Math.max(0L, override.getMaxTokensPerDay());
            double maxCost = override.getMaxCostUsdPerDay() == null ? 0.0D : Math.max(0.0D, override.getMaxCostUsdPerDay());
            AiQuotaScopedPolicy policy = new AiQuotaScopedPolicy(
                    scopeKey,
                    override.getScopeType().trim().toUpperCase(Locale.ROOT),
                    scopeLabel(override),
                    maxCalls,
                    maxTokens,
                    maxCost);
            if (policy.active()) {
                scopes.put(scopeKey, policy);
            }
        }
        return scopes;
    }

    /**
     * @param providerType remote provider type for the in-flight call
     * @param quota        configured quota block
     * @return scoped policies that apply to the current call context
     */
    static List<AiQuotaScopedPolicy> activeCallScopes(String providerType, DataGeneratorProperties.AiRuntimeQuota quota) {
        Map<String, AiQuotaScopedPolicy> configured = configuredScopes(quota);
        List<AiQuotaScopedPolicy> active = new ArrayList<>();
        if (providerType != null && !providerType.isBlank()) {
            AiQuotaScopedPolicy providerPolicy = configured.get(AiQuotaScopeKeys.provider(providerType));
            if (providerPolicy != null) {
                active.add(providerPolicy);
            }
        }
        Long templateId = AiExecutionScope.templateId();
        if (templateId != null) {
            AiQuotaScopedPolicy templatePolicy = configured.get(AiQuotaScopeKeys.template(String.valueOf(templateId)));
            if (templatePolicy != null) {
                active.add(templatePolicy);
            }
        }
        String tenantId = AiExecutionScope.tenantId();
        if (tenantId != null) {
            AiQuotaScopedPolicy tenantPolicy = configured.get(AiQuotaScopeKeys.tenant(tenantId));
            if (tenantPolicy != null) {
                active.add(tenantPolicy);
            }
        }
        return active;
    }

    private static String scopeLabel(DataGeneratorProperties.AiQuotaScopeOverride override) {
        String type = override.getScopeType().trim().toUpperCase(Locale.ROOT);
        String key = override.getScopeKey().trim();
        if ("PROVIDER".equals(type)) {
            return key.toUpperCase(Locale.ROOT);
        }
        if ("TENANT".equals(type)) {
            String tenantId = AiExecutionScope.tenantId();
            if (tenantId != null && !tenantId.isBlank()) {
                return tenantId;
            }
            return key;
        }
        String templateName = AiExecutionScope.templateName();
        if (templateName != null && !templateName.isBlank()) {
            return templateName;
        }
        return key;
    }
}
