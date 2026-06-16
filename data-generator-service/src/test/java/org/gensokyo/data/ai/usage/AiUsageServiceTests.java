/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.usage;

import org.gensokyo.data.api.console.dto.AiUsageSummaryDto;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.po.TaskExecutionPO;
import org.gensokyo.data.model.v2.AiCallMetricVO;
import org.gensokyo.data.model.v2.RunReportVO;
import org.gensokyo.data.repository.TaskExecutionRepository;
import org.gensokyo.data.task.TaskExecutionStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AiUsageService}.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
@ExtendWith(MockitoExtension.class)
class AiUsageServiceTests {

    @Mock
    private TaskExecutionRepository taskExecutionRepository;

    @Mock
    private AiPricingService aiPricingService;

    @InjectMocks
    private AiUsageService aiUsageService;

    @Test
    void summarizeAggregatesAiCallsFromSuccessfulReports() {
        RunReportVO report = new RunReportVO(
                List.of(),
                List.of(),
                List.of(),
                "LOCAL",
                42L,
                List.of(),
                List.of(new AiCallMetricVO(
                        "ai_seed",
                        "OLLAMA",
                        "qwen2",
                        10L,
                        5L,
                        20L,
                        1,
                        "sample",
                        0.0D)));
        TaskExecutionPO row = new TaskExecutionPO();
        row.setStatus(TaskExecutionStatus.SUCCESS.name());
        row.setReportJson(TemplateJsonCodec.write(report));
        when(taskExecutionRepository.findByStatusOrderByFinishedAtDesc(TaskExecutionStatus.SUCCESS.name()))
                .thenReturn(List.of(row));

        AiUsageSummaryDto summary = aiUsageService.summarize();

        Assertions.assertEquals(1L, summary.jobsWithAiCalls());
        Assertions.assertEquals(1L, summary.totalCalls());
        Assertions.assertEquals(10L, summary.promptTokens());
        Assertions.assertEquals(5L, summary.completionTokens());
        Assertions.assertEquals(20L, summary.totalLatencyMs());
        Assertions.assertEquals(0.0D, summary.estimatedCostUsd());
        Assertions.assertEquals(1, summary.byProvider().size());
        Assertions.assertEquals("OLLAMA", summary.byProvider().getFirst().providerType());
    }

    @Test
    void summarizeRecomputesCostForLegacyReportsWithoutStoredEstimate() {
        RunReportVO report = new RunReportVO(
                List.of(),
                List.of(),
                List.of(),
                "LOCAL",
                42L,
                List.of(),
                List.of(new AiCallMetricVO(
                        "ai_seed",
                        "OPENAI",
                        "gpt-4o-mini",
                        1_000_000L,
                        0L,
                        20L,
                        1,
                        "sample",
                        null)));
        TaskExecutionPO row = new TaskExecutionPO();
        row.setStatus(TaskExecutionStatus.SUCCESS.name());
        row.setReportJson(TemplateJsonCodec.write(report));
        when(taskExecutionRepository.findByStatusOrderByFinishedAtDesc(TaskExecutionStatus.SUCCESS.name()))
                .thenReturn(List.of(row));
        when(aiPricingService.estimateUsd("OPENAI", "gpt-4o-mini", 1_000_000L, 0L)).thenReturn(0.15D);

        AiUsageSummaryDto summary = aiUsageService.summarize();

        Assertions.assertEquals(0.15D, summary.estimatedCostUsd(), 0.000001D);
        Assertions.assertEquals(0.15D, summary.byProvider().getFirst().estimatedCostUsd(), 0.000001D);
    }
}
