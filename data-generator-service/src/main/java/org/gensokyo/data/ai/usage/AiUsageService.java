/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.usage;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.api.console.dto.AiProviderUsageDto;
import org.gensokyo.data.api.console.dto.AiUsageSummaryDto;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.po.TaskExecutionPO;
import org.gensokyo.data.model.v2.AiCallMetricVO;
import org.gensokyo.data.model.v2.RunReportVO;
import org.gensokyo.data.repository.TaskExecutionRepository;
import org.gensokyo.data.task.TaskExecutionStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates AI call metrics from persisted successful job run reports.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
@Service
@RequiredArgsConstructor
public class AiUsageService {

    private final TaskExecutionRepository taskExecutionRepository;
    private final AiPricingService aiPricingService;

    /**
     * @return platform-level AI usage totals from all successful executions with reports
     */
    public AiUsageSummaryDto summarize() {
        List<TaskExecutionPO> successes = taskExecutionRepository.findByStatusOrderByFinishedAtDesc(
                TaskExecutionStatus.SUCCESS.name());

        long jobsWithAiCalls = 0L;
        long totalCalls = 0L;
        long promptTokens = 0L;
        long completionTokens = 0L;
        long totalLatencyMs = 0L;
        BigDecimal totalCostUsd = BigDecimal.ZERO;
        Map<String, ProviderAccumulator> byProvider = new LinkedHashMap<>();

        for (TaskExecutionPO row : successes) {
            RunReportVO report = parseReport(row.getReportJson());
            if (report == null || report.aiCalls() == null || report.aiCalls().isEmpty()) {
                continue;
            }
            jobsWithAiCalls++;
            for (AiCallMetricVO call : report.aiCalls()) {
                totalCalls++;
                long prompt = call.promptTokens() == null ? 0L : call.promptTokens();
                long completion = call.completionTokens() == null ? 0L : call.completionTokens();
                long latency = call.latencyMs() == null ? 0L : call.latencyMs();
                double callCostUsd = resolveEstimatedCostUsd(call, prompt, completion);
                promptTokens += prompt;
                completionTokens += completion;
                totalLatencyMs += latency;
                totalCostUsd = totalCostUsd.add(BigDecimal.valueOf(callCostUsd));

                String providerType = call.providerType() == null ? "UNKNOWN" : call.providerType();
                byProvider.computeIfAbsent(providerType, ignored -> new ProviderAccumulator())
                        .add(prompt, completion, latency, callCostUsd);
            }
        }

        List<AiProviderUsageDto> providerRows = new ArrayList<>();
        byProvider.forEach((providerType, totals) -> providerRows.add(new AiProviderUsageDto(
                providerType,
                totals.calls,
                totals.promptTokens,
                totals.completionTokens,
                totals.latencyMs,
                totals.estimatedCostUsd)));
        providerRows.sort(Comparator.comparingLong(AiProviderUsageDto::calls).reversed());

        return new AiUsageSummaryDto(
                jobsWithAiCalls,
                totalCalls,
                promptTokens,
                completionTokens,
                totalLatencyMs,
                totalCostUsd.setScale(6, RoundingMode.HALF_UP).doubleValue(),
                List.copyOf(providerRows));
    }

    private double resolveEstimatedCostUsd(AiCallMetricVO call, long promptTokens, long completionTokens) {
        if (call.estimatedCostUsd() != null) {
            return call.estimatedCostUsd();
        }
        return aiPricingService.estimateUsd(call.providerType(), call.model(), promptTokens, completionTokens);
    }

    private static RunReportVO parseReport(String reportJson) {
        if (reportJson == null || reportJson.isBlank()) {
            return null;
        }
        try {
            return TemplateJsonCodec.read(reportJson, RunReportVO.class);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static final class ProviderAccumulator {
        private long calls;
        private long promptTokens;
        private long completionTokens;
        private long latencyMs;
        private double estimatedCostUsd;

        private void add(long prompt, long completion, long latency, double costUsd) {
            calls++;
            promptTokens += prompt;
            completionTokens += completion;
            latencyMs += latency;
            estimatedCostUsd = BigDecimal.valueOf(estimatedCostUsd)
                    .add(BigDecimal.valueOf(costUsd))
                    .setScale(6, RoundingMode.HALF_UP)
                    .doubleValue();
        }
    }
}
