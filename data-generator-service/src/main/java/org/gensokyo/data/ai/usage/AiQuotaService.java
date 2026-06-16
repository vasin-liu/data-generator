/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.usage;

import org.gensokyo.data.api.console.dto.AiQuotaStatusDto;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.model.po.AiQuotaDailyUsagePO;
import org.gensokyo.data.repository.AiQuotaDailyUsageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Enforces and tracks platform AI daily quotas across JVMs via JDBC counters.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
@Service
public class AiQuotaService {

    private final DataGeneratorProperties properties;
    private final AiQuotaDailyUsageRepository repository;
    private final AiPricingService pricingService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    /**
     * @param properties          platform configuration
     * @param repository          daily usage store
     * @param pricingService      cost estimator for quota accounting
     * @param transactionTemplate short quota transactions
     */
    @Autowired
    public AiQuotaService(
            DataGeneratorProperties properties,
            AiQuotaDailyUsageRepository repository,
            AiPricingService pricingService,
            TransactionTemplate transactionTemplate) {
        this(properties, repository, pricingService, transactionTemplate, Clock.systemUTC());
    }

    /**
     * Package-private for tests with a fixed clock.
     */
    AiQuotaService(
            DataGeneratorProperties properties,
            AiQuotaDailyUsageRepository repository,
            AiPricingService pricingService,
            TransactionTemplate transactionTemplate,
            Clock clock) {
        this.properties = properties;
        this.repository = repository;
        this.pricingService = pricingService;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * @return current UTC-day quota status for the operator console
     */
    public AiQuotaStatusDto status() {
        AiQuotaPolicy policy = policy();
        String usageDate = usageDate();
        AiQuotaDailyUsagePO row = repository.findById(usageDate).orElseGet(() -> emptyRow(usageDate));
        long usedTokens = row.getPromptTokens() + row.getCompletionTokens();
        return new AiQuotaStatusDto(
                policy.enabled(),
                usageDate,
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

    /**
     * Reserves one AI call against the daily quota before a remote provider request.
     */
    public void beforeCall() {
        AiQuotaPolicy policy = policy();
        if (!policy.enabled()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            AiQuotaDailyUsagePO row = lockedRow(usageDate());
            assertWithinLimits(policy, row, 1L, 0L, 0.0D);
            row.setCallCount(row.getCallCount() + 1L);
            repository.save(row);
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
        AiQuotaPolicy policy = policy();
        if (!policy.enabled()) {
            return;
        }
        double costUsd = pricingService.estimateUsd(providerType, model, promptTokens, completionTokens);
        transactionTemplate.executeWithoutResult(status -> {
            AiQuotaDailyUsagePO row = lockedRow(usageDate());
            row.setPromptTokens(row.getPromptTokens() + promptTokens);
            row.setCompletionTokens(row.getCompletionTokens() + completionTokens);
            row.setEstimatedCostUsd(roundUsd(row.getEstimatedCostUsd() + costUsd));
            repository.save(row);
        });
    }

    private AiQuotaPolicy policy() {
        DataGeneratorProperties.AiRuntime aiRuntime = properties == null
                ? new DataGeneratorProperties.AiRuntime()
                : properties.getAiRuntime();
        return AiQuotaPolicy.resolve(aiRuntime == null ? null : aiRuntime.getQuota());
    }

    private String usageDate() {
        return LocalDate.now(clock).toString();
    }

    private AiQuotaDailyUsagePO lockedRow(String usageDate) {
        return repository.findLockedByUsageDate(usageDate).orElseGet(() -> {
            AiQuotaDailyUsagePO created = emptyRow(usageDate);
            return repository.save(created);
        });
    }

    private static AiQuotaDailyUsagePO emptyRow(String usageDate) {
        AiQuotaDailyUsagePO row = new AiQuotaDailyUsagePO();
        row.setUsageDate(usageDate);
        row.setCallCount(0L);
        row.setPromptTokens(0L);
        row.setCompletionTokens(0L);
        row.setEstimatedCostUsd(0.0D);
        return row;
    }

    private static void assertWithinLimits(
            AiQuotaPolicy policy,
            AiQuotaDailyUsagePO row,
            long additionalCalls,
            long additionalTokens,
            double additionalCostUsd) {
        long usedTokens = row.getPromptTokens() + row.getCompletionTokens();
        if (policy.maxCallsPerDay() > 0L && row.getCallCount() + additionalCalls > policy.maxCallsPerDay()) {
            throw new AiQuotaExceededException(
                    "Platform AI daily call quota exceeded (" + row.getCallCount() + "/" + policy.maxCallsPerDay() + ")");
        }
        if (policy.maxTokensPerDay() > 0L && usedTokens + additionalTokens > policy.maxTokensPerDay()) {
            throw new AiQuotaExceededException(
                    "Platform AI daily token quota exceeded (" + usedTokens + "/" + policy.maxTokensPerDay() + ")");
        }
        if (policy.maxCostUsdPerDay() > 0.0D
                && row.getEstimatedCostUsd() + additionalCostUsd > policy.maxCostUsdPerDay() + 0.000001D) {
            throw new AiQuotaExceededException(
                    "Platform AI daily cost quota exceeded ("
                            + roundUsd(row.getEstimatedCostUsd()) + "/" + policy.maxCostUsdPerDay() + " USD)");
        }
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
