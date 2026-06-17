/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.usage;

import org.gensokyo.data.api.console.dto.AiQuotaScopeStatusDto;
import org.gensokyo.data.api.console.dto.AiQuotaStatusDto;
import org.gensokyo.data.audit.AuditService;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.model.po.AiQuotaDailyUsagePO;
import org.gensokyo.data.model.po.AiQuotaScopeDailyUsageId;
import org.gensokyo.data.model.po.AiQuotaScopeDailyUsagePO;
import org.gensokyo.data.repository.AiQuotaDailyUsageRepository;
import org.gensokyo.data.repository.AiQuotaScopeDailyUsageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces and tracks platform, provider, and template AI daily quotas across JVMs via JDBC counters.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
@Service
public class AiQuotaService {

    private final DataGeneratorProperties properties;
    private final AiQuotaDailyUsageRepository platformRepository;
    private final AiQuotaScopeDailyUsageRepository scopedRepository;
    private final AiPricingService pricingService;
    private final AuditService auditService;
    private final AiQuotaWebhookNotifier webhookNotifier;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final Set<String> alertedKeys = ConcurrentHashMap.newKeySet();

    /**
     * @param properties          platform configuration
     * @param platformRepository  platform daily usage store
     * @param scopedRepository    scoped daily usage store
     * @param pricingService      cost estimator for quota accounting
     * @param auditService        audit log for quota alert hooks
     * @param webhookNotifier     outbound quota webhook dispatcher
     * @param transactionTemplate short quota transactions
     */
    @Autowired
    public AiQuotaService(
            DataGeneratorProperties properties,
            AiQuotaDailyUsageRepository platformRepository,
            AiQuotaScopeDailyUsageRepository scopedRepository,
            AiPricingService pricingService,
            AuditService auditService,
            AiQuotaWebhookNotifier webhookNotifier,
            TransactionTemplate transactionTemplate) {
        this(properties, platformRepository, scopedRepository, pricingService, auditService, webhookNotifier, transactionTemplate, Clock.systemUTC());
    }

    /**
     * Package-private for tests with a fixed clock.
     */
    AiQuotaService(
            DataGeneratorProperties properties,
            AiQuotaDailyUsageRepository platformRepository,
            AiQuotaScopeDailyUsageRepository scopedRepository,
            AiPricingService pricingService,
            AuditService auditService,
            AiQuotaWebhookNotifier webhookNotifier,
            TransactionTemplate transactionTemplate,
            Clock clock) {
        this.properties = properties;
        this.platformRepository = platformRepository;
        this.scopedRepository = scopedRepository;
        this.pricingService = pricingService;
        this.auditService = auditService;
        this.webhookNotifier = webhookNotifier;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * @return current UTC-day quota status for the operator console
     */
    public AiQuotaStatusDto status() {
        AiQuotaPolicy platformPolicy = platformPolicy();
        DataGeneratorProperties.AiRuntimeQuota quotaConfig = quotaConfig();
        boolean quotaEnabled = quotaConfig.isEnabled();
        var configuredScopes = AiQuotaScopePolicyResolver.configuredScopes(quotaConfig);
        String usageDate = usageDate();
        AiQuotaDailyUsagePO platformRow = platformRepository.findById(usageDate).orElseGet(() -> emptyPlatformRow(usageDate));
        long usedTokens = platformRow.getPromptTokens() + platformRow.getCompletionTokens();
        List<AiQuotaScopeStatusDto> scopes = new ArrayList<>();
        for (AiQuotaScopedPolicy scopedPolicy : configuredScopes.values()) {
            AiQuotaScopeDailyUsagePO scopedRow = scopedRepository.findById(scopeId(usageDate, scopedPolicy.scopeKey()))
                    .orElseGet(() -> emptyScopedRow(usageDate, scopedPolicy.scopeKey()));
            scopes.add(toScopeStatus(scopedPolicy, scopedRow));
        }
        return new AiQuotaStatusDto(
                quotaEnabled && (platformPolicy.enabled() || !configuredScopes.isEmpty()),
                usageDate,
                platformPolicy.maxCallsPerDay(),
                platformPolicy.maxTokensPerDay(),
                platformPolicy.maxCostUsdPerDay(),
                platformRow.getCallCount(),
                platformRow.getPromptTokens(),
                platformRow.getCompletionTokens(),
                roundUsd(platformRow.getEstimatedCostUsd()),
                remaining(platformPolicy.maxCallsPerDay(), platformRow.getCallCount()),
                remaining(platformPolicy.maxTokensPerDay(), usedTokens),
                remainingCost(platformPolicy.maxCostUsdPerDay(), platformRow.getEstimatedCostUsd()),
                alertsEnabled(quotaConfig),
                warnAtPercent(quotaConfig),
                scopes,
                webhooksEnabled(quotaConfig),
                webhookCount(quotaConfig));
    }

    /**
     * Reserves one AI call against applicable daily quotas before a remote provider request.
     *
     * @param providerType provider identifier for scoped enforcement
     */
    public void beforeCall(String providerType) {
        DataGeneratorProperties.AiRuntimeQuota quotaConfig = quotaConfig();
        if (!quotaConfig.isEnabled()) {
            return;
        }
        AiQuotaPolicy platformPolicy = platformPolicy();
        List<AiQuotaScopedPolicy> scopedPolicies = AiQuotaScopePolicyResolver.activeCallScopes(providerType, quotaConfig);
        if (!platformPolicy.enabled() && scopedPolicies.isEmpty()) {
            return;
        }
        String usageDate = usageDate();
        transactionTemplate.executeWithoutResult(status -> {
            if (platformPolicy.enabled()) {
                AiQuotaDailyUsagePO row = lockedPlatformRow(usageDate);
                reserveCall(platformPolicy, row, usageDate, AiQuotaScopeKeys.PLATFORM, "PLATFORM");
                platformRepository.save(row);
            }
            for (AiQuotaScopedPolicy scopedPolicy : scopedPolicies) {
                AiQuotaScopeDailyUsagePO row = lockedScopedRow(usageDate, scopedPolicy.scopeKey());
                reserveCall(scopedPolicy, row, usageDate, scopedPolicy.scopeKey(), scopedPolicy.scopeType());
                scopedRepository.save(row);
            }
        });
    }

    /**
     * Records token and USD usage after a successful remote AI call.
     *
     * @param providerType     provider identifier
     * @param model            resolved model name
     * @param promptTokens     prompt tokens consumed
     * @param completionTokens completion tokens consumed
     */
    public void recordUsage(String providerType, String model, long promptTokens, long completionTokens) {
        DataGeneratorProperties.AiRuntimeQuota quotaConfig = quotaConfig();
        if (!quotaConfig.isEnabled()) {
            return;
        }
        AiQuotaPolicy platformPolicy = platformPolicy();
        List<AiQuotaScopedPolicy> scopedPolicies = AiQuotaScopePolicyResolver.activeCallScopes(providerType, quotaConfig);
        if (!platformPolicy.enabled() && scopedPolicies.isEmpty()) {
            return;
        }
        double costUsd = pricingService.estimateUsd(providerType, model, promptTokens, completionTokens);
        String usageDate = usageDate();
        transactionTemplate.executeWithoutResult(status -> {
            if (platformPolicy.enabled()) {
                AiQuotaDailyUsagePO row = lockedPlatformRow(usageDate);
                recordTokenUsage(platformPolicy, row, usageDate, AiQuotaScopeKeys.PLATFORM, "PLATFORM", promptTokens, completionTokens, costUsd);
                platformRepository.save(row);
            }
            for (AiQuotaScopedPolicy scopedPolicy : scopedPolicies) {
                AiQuotaScopeDailyUsagePO row = lockedScopedRow(usageDate, scopedPolicy.scopeKey());
                recordTokenUsage(scopedPolicy, row, usageDate, scopedPolicy.scopeKey(), scopedPolicy.scopeType(), promptTokens, completionTokens, costUsd);
                scopedRepository.save(row);
            }
        });
    }

    private void reserveCall(
            AiQuotaPolicy policy,
            AiQuotaDailyUsagePO row,
            String usageDate,
            String scopeKey,
            String scopeType) {
        assertWithinLimits(policy, row, 1L, 0L, 0.0D, usageDate, scopeKey, scopeType);
        row.setCallCount(row.getCallCount() + 1L);
        maybeWarn(policy, row, usageDate, scopeKey, scopeType, "CALLS");
    }

    private void reserveCall(
            AiQuotaScopedPolicy policy,
            AiQuotaScopeDailyUsagePO row,
            String usageDate,
            String scopeKey,
            String scopeType) {
        assertWithinLimits(policy, row, 1L, 0L, 0.0D, usageDate, scopeKey, scopeType);
        row.setCallCount(row.getCallCount() + 1L);
        maybeWarn(policy, row, usageDate, scopeKey, scopeType, "CALLS");
    }

    private void recordTokenUsage(
            AiQuotaPolicy policy,
            AiQuotaDailyUsagePO row,
            String usageDate,
            String scopeKey,
            String scopeType,
            long promptTokens,
            long completionTokens,
            double additionalCostUsd) {
        long additionalTokens = promptTokens + completionTokens;
        assertWithinLimits(policy, row, 0L, additionalTokens, additionalCostUsd, usageDate, scopeKey, scopeType);
        row.setPromptTokens(row.getPromptTokens() + promptTokens);
        row.setCompletionTokens(row.getCompletionTokens() + completionTokens);
        row.setEstimatedCostUsd(roundUsd(row.getEstimatedCostUsd() + additionalCostUsd));
        maybeWarn(policy, row, usageDate, scopeKey, scopeType, "TOKENS");
        maybeWarn(policy, row, usageDate, scopeKey, scopeType, "COST");
    }

    private void recordTokenUsage(
            AiQuotaScopedPolicy policy,
            AiQuotaScopeDailyUsagePO row,
            String usageDate,
            String scopeKey,
            String scopeType,
            long promptTokens,
            long completionTokens,
            double additionalCostUsd) {
        long additionalTokens = promptTokens + completionTokens;
        assertWithinLimits(policy, row, 0L, additionalTokens, additionalCostUsd, usageDate, scopeKey, scopeType);
        row.setPromptTokens(row.getPromptTokens() + promptTokens);
        row.setCompletionTokens(row.getCompletionTokens() + completionTokens);
        row.setEstimatedCostUsd(roundUsd(row.getEstimatedCostUsd() + additionalCostUsd));
        maybeWarn(policy, row, usageDate, scopeKey, scopeType, "TOKENS");
        maybeWarn(policy, row, usageDate, scopeKey, scopeType, "COST");
    }

    private void assertWithinLimits(
            AiQuotaPolicy policy,
            AiQuotaDailyUsagePO row,
            long additionalCalls,
            long additionalTokens,
            double additionalCostUsd,
            String usageDate,
            String scopeKey,
            String scopeType) {
        long usedTokens = row.getPromptTokens() + row.getCompletionTokens();
        if (policy.maxCallsPerDay() > 0L && row.getCallCount() + additionalCalls > policy.maxCallsPerDay()) {
            recordExceeded(usageDate, scopeKey, scopeType, "CALLS", row.getCallCount(), policy.maxCallsPerDay());
            throw exceeded("call", row.getCallCount(), policy.maxCallsPerDay(), scopeKey);
        }
        if (policy.maxTokensPerDay() > 0L && usedTokens + additionalTokens > policy.maxTokensPerDay()) {
            recordExceeded(usageDate, scopeKey, scopeType, "TOKENS", usedTokens, policy.maxTokensPerDay());
            throw exceeded("token", usedTokens, policy.maxTokensPerDay(), scopeKey);
        }
        if (policy.maxCostUsdPerDay() > 0.0D
                && row.getEstimatedCostUsd() + additionalCostUsd > policy.maxCostUsdPerDay() + 0.000001D) {
            recordExceeded(usageDate, scopeKey, scopeType, "COST", roundUsd(row.getEstimatedCostUsd()), policy.maxCostUsdPerDay());
            throw exceeded("cost", roundUsd(row.getEstimatedCostUsd()), policy.maxCostUsdPerDay(), scopeKey);
        }
    }

    private void assertWithinLimits(
            AiQuotaScopedPolicy policy,
            AiQuotaScopeDailyUsagePO row,
            long additionalCalls,
            long additionalTokens,
            double additionalCostUsd,
            String usageDate,
            String scopeKey,
            String scopeType) {
        long usedTokens = row.getPromptTokens() + row.getCompletionTokens();
        if (policy.maxCallsPerDay() > 0L && row.getCallCount() + additionalCalls > policy.maxCallsPerDay()) {
            recordExceeded(usageDate, scopeKey, scopeType, "CALLS", row.getCallCount(), policy.maxCallsPerDay());
            throw exceeded("call", row.getCallCount(), policy.maxCallsPerDay(), scopeKey);
        }
        if (policy.maxTokensPerDay() > 0L && usedTokens + additionalTokens > policy.maxTokensPerDay()) {
            recordExceeded(usageDate, scopeKey, scopeType, "TOKENS", usedTokens, policy.maxTokensPerDay());
            throw exceeded("token", usedTokens, policy.maxTokensPerDay(), scopeKey);
        }
        if (policy.maxCostUsdPerDay() > 0.0D
                && row.getEstimatedCostUsd() + additionalCostUsd > policy.maxCostUsdPerDay() + 0.000001D) {
            recordExceeded(usageDate, scopeKey, scopeType, "COST", roundUsd(row.getEstimatedCostUsd()), policy.maxCostUsdPerDay());
            throw exceeded("cost", roundUsd(row.getEstimatedCostUsd()), policy.maxCostUsdPerDay(), scopeKey);
        }
    }

    private void maybeWarn(
            AiQuotaPolicy policy,
            AiQuotaDailyUsagePO row,
            String usageDate,
            String scopeKey,
            String scopeType,
            String dimension) {
        maybeWarn(usageDate, scopeKey, scopeType, dimension, usedValue(row, dimension), maxValue(policy, dimension));
    }

    private void maybeWarn(
            AiQuotaScopedPolicy policy,
            AiQuotaScopeDailyUsagePO row,
            String usageDate,
            String scopeKey,
            String scopeType,
            String dimension) {
        maybeWarn(usageDate, scopeKey, scopeType, dimension, usedValue(row, dimension), maxValue(policy, dimension));
    }

    private void maybeWarn(
            String usageDate,
            String scopeKey,
            String scopeType,
            String dimension,
            double used,
            double max) {
        DataGeneratorProperties.AiRuntimeQuota quotaConfig = quotaConfig();
        if (!alertsEnabled(quotaConfig) && !webhooksEnabled(quotaConfig)) {
            return;
        }
        int warnAtPercent = warnAtPercent(quotaConfig);
        if (warnAtPercent <= 0 || max <= 0.0D) {
            return;
        }
        double percentUsed = used / max * 100.0D;
        if (percentUsed < warnAtPercent) {
            return;
        }
        String alertKey = usageDate + "|" + scopeKey + "|WARN|" + dimension;
        if (!alertedKeys.add(alertKey)) {
            return;
        }
        emitWarn(usageDate, scopeKey, scopeType, dimension, used, max, roundUsd(percentUsed));
    }

    private void emitWarn(
            String usageDate,
            String scopeKey,
            String scopeType,
            String dimension,
            double used,
            double max,
            double percentUsed) {
        if (alertsEnabled(quotaConfig())) {
            auditService.record(
                    "AI_QUOTA_WARN",
                    "AI_QUOTA",
                    scopeKey,
                    Map.of(
                            "usageDate", usageDate,
                            "scopeType", scopeType,
                            "dimension", dimension,
                            "used", used,
                            "max", max,
                            "percentUsed", percentUsed));
        }
        if (webhookNotifier != null) {
            webhookNotifier.notifyEvent(new AiQuotaAlertEvent(
                    "WARN", usageDate, scopeKey, scopeType, dimension, used, max, percentUsed));
        }
    }

    private void recordExceeded(
            String usageDate,
            String scopeKey,
            String scopeType,
            String dimension,
            double used,
            double max) {
        if (alertsEnabled(quotaConfig())) {
            auditService.record(
                    "AI_QUOTA_EXCEEDED",
                    "AI_QUOTA",
                    scopeKey,
                    Map.of(
                            "usageDate", usageDate,
                            "scopeType", scopeType,
                            "dimension", dimension,
                            "used", used,
                            "max", max));
        }
        if (webhookNotifier != null) {
            webhookNotifier.notifyEvent(new AiQuotaAlertEvent(
                    "EXCEEDED", usageDate, scopeKey, scopeType, dimension, used, max, null));
        }
    }

    private AiQuotaPolicy platformPolicy() {
        DataGeneratorProperties.AiRuntime aiRuntime = properties == null
                ? new DataGeneratorProperties.AiRuntime()
                : properties.getAiRuntime();
        return AiQuotaPolicy.resolve(aiRuntime == null ? null : aiRuntime.getQuota());
    }

    private DataGeneratorProperties.AiRuntimeQuota quotaConfig() {
        DataGeneratorProperties.AiRuntime aiRuntime = properties == null
                ? new DataGeneratorProperties.AiRuntime()
                : properties.getAiRuntime();
        return aiRuntime == null || aiRuntime.getQuota() == null
                ? new DataGeneratorProperties.AiRuntimeQuota()
                : aiRuntime.getQuota();
    }

    private String usageDate() {
        return LocalDate.now(clock).toString();
    }

    private AiQuotaDailyUsagePO lockedPlatformRow(String usageDate) {
        return platformRepository.findLockedByUsageDate(usageDate).orElseGet(() -> {
            AiQuotaDailyUsagePO created = emptyPlatformRow(usageDate);
            return platformRepository.save(created);
        });
    }

    private AiQuotaScopeDailyUsagePO lockedScopedRow(String usageDate, String scopeKey) {
        return scopedRepository.findLockedByUsageDateAndScopeKey(usageDate, scopeKey).orElseGet(() -> {
            AiQuotaScopeDailyUsagePO created = emptyScopedRow(usageDate, scopeKey);
            return scopedRepository.save(created);
        });
    }

    private static AiQuotaScopeDailyUsageId scopeId(String usageDate, String scopeKey) {
        AiQuotaScopeDailyUsageId id = new AiQuotaScopeDailyUsageId();
        id.setUsageDate(usageDate);
        id.setScopeKey(scopeKey);
        return id;
    }

    private static AiQuotaDailyUsagePO emptyPlatformRow(String usageDate) {
        AiQuotaDailyUsagePO row = new AiQuotaDailyUsagePO();
        row.setUsageDate(usageDate);
        row.setCallCount(0L);
        row.setPromptTokens(0L);
        row.setCompletionTokens(0L);
        row.setEstimatedCostUsd(0.0D);
        return row;
    }

    private static AiQuotaScopeDailyUsagePO emptyScopedRow(String usageDate, String scopeKey) {
        AiQuotaScopeDailyUsagePO row = new AiQuotaScopeDailyUsagePO();
        row.setId(scopeId(usageDate, scopeKey));
        row.setCallCount(0L);
        row.setPromptTokens(0L);
        row.setCompletionTokens(0L);
        row.setEstimatedCostUsd(0.0D);
        return row;
    }

    private static AiQuotaExceededException exceeded(String dimension, double used, double max, String scopeKey) {
        return new AiQuotaExceededException(
                "AI daily " + dimension + " quota exceeded for [" + scopeKey + "] (" + used + "/" + max + ")");
    }

    private static double usedValue(AiQuotaDailyUsagePO row, String dimension) {
        return switch (dimension) {
            case "CALLS" -> row.getCallCount();
            case "TOKENS" -> row.getPromptTokens() + row.getCompletionTokens();
            case "COST" -> row.getEstimatedCostUsd();
            default -> 0.0D;
        };
    }

    private static double usedValue(AiQuotaScopeDailyUsagePO row, String dimension) {
        return switch (dimension) {
            case "CALLS" -> row.getCallCount();
            case "TOKENS" -> row.getPromptTokens() + row.getCompletionTokens();
            case "COST" -> row.getEstimatedCostUsd();
            default -> 0.0D;
        };
    }

    private static double maxValue(AiQuotaPolicy policy, String dimension) {
        return switch (dimension) {
            case "CALLS" -> policy.maxCallsPerDay();
            case "TOKENS" -> policy.maxTokensPerDay();
            case "COST" -> policy.maxCostUsdPerDay();
            default -> 0.0D;
        };
    }

    private static double maxValue(AiQuotaScopedPolicy policy, String dimension) {
        return switch (dimension) {
            case "CALLS" -> policy.maxCallsPerDay();
            case "TOKENS" -> policy.maxTokensPerDay();
            case "COST" -> policy.maxCostUsdPerDay();
            default -> 0.0D;
        };
    }

    private static AiQuotaScopeStatusDto toScopeStatus(AiQuotaScopedPolicy policy, AiQuotaScopeDailyUsagePO row) {
        long usedTokens = row.getPromptTokens() + row.getCompletionTokens();
        return new AiQuotaScopeStatusDto(
                policy.scopeKey(),
                policy.scopeType(),
                policy.scopeLabel(),
                policy.maxCallsPerDay(),
                policy.maxTokensPerDay(),
                policy.maxCostUsdPerDay(),
                row.getCallCount(),
                row.getPromptTokens(),
                row.getCompletionTokens(),
                roundUsd(row.getEstimatedCostUsd()),
                remaining(policy.maxCallsPerDay(), row.getCallCount()),
                remaining(policy.maxTokensPerDay(), usedTokens),
                remainingCost(policy.maxCostUsdPerDay(), row.getEstimatedCostUsd()));
    }

    private static boolean alertsEnabled(DataGeneratorProperties.AiRuntimeQuota quota) {
        return quota != null && quota.isAlertsEnabled();
    }

    private static int warnAtPercent(DataGeneratorProperties.AiRuntimeQuota quota) {
        if (quota == null || quota.getWarnAtPercent() == null) {
            return 80;
        }
        return Math.max(0, Math.min(100, quota.getWarnAtPercent()));
    }

    private static boolean webhooksEnabled(DataGeneratorProperties.AiRuntimeQuota quota) {
        return quota != null && quota.isWebhooksEnabled() && quota.getWebhooks() != null && !quota.getWebhooks().isEmpty();
    }

    private static int webhookCount(DataGeneratorProperties.AiRuntimeQuota quota) {
        if (quota == null || quota.getWebhooks() == null) {
            return 0;
        }
        return (int) quota.getWebhooks().stream()
                .filter(endpoint -> endpoint != null && endpoint.getUrl() != null && !endpoint.getUrl().isBlank())
                .count();
    }

    private static Long remaining(long max, long used) {
        if (max <= 0L) {
            return null;
        }
        return Math.max(0L, max - used);
    }

    private static Double remainingCost(double max, double used) {
        if (max <= 0.0D) {
            return null;
        }
        return Math.max(0.0D, roundUsd(max - used));
    }

    private static double roundUsd(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }
}
