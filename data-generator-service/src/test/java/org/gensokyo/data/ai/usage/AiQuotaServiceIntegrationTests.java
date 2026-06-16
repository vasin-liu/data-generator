/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ai.usage;

import org.gensokyo.data.DataGeneratorApplication;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.repository.AiQuotaDailyUsageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Integration tests for {@link AiQuotaService}.
 *
 * @author Gensokyo
 * @since 2026-06-12
 */
@SpringBootTest(
        classes = DataGeneratorApplication.class,
        properties = "spring.config.location=classpath:/application-phase7-test.yaml"
)
class AiQuotaServiceIntegrationTests {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-12T10:15:30Z");

    @Autowired
    private AiQuotaDailyUsageRepository repository;

    @Autowired
    private AiPricingService pricingService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DataGeneratorProperties properties;

    @AfterEach
    void cleanup() {
        repository.deleteAll();
        properties.getAiRuntime().getQuota().setEnabled(false);
        properties.getAiRuntime().getQuota().setMaxCallsPerDay(0L);
        properties.getAiRuntime().getQuota().setMaxTokensPerDay(0L);
    }

    @Test
    void enforcesDailyCallQuotaAcrossServiceInstances() {
        properties.getAiRuntime().getQuota().setEnabled(true);
        properties.getAiRuntime().getQuota().setMaxCallsPerDay(1L);

        AiQuotaService quotaService = new AiQuotaService(
                properties, repository, pricingService, transactionTemplate, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));

        quotaService.beforeCall();
        quotaService.recordUsage("OPENAI", "gpt-4o-mini", 10L, 5L);

        AiQuotaExceededException failure = Assertions.assertThrows(
                AiQuotaExceededException.class,
                quotaService::beforeCall);
        Assertions.assertTrue(failure.getMessage().contains("call quota exceeded"));
    }

    @Test
    void statusReportsRemainingQuota() {
        properties.getAiRuntime().getQuota().setEnabled(true);
        properties.getAiRuntime().getQuota().setMaxCallsPerDay(5L);
        properties.getAiRuntime().getQuota().setMaxTokensPerDay(100L);

        AiQuotaService quotaService = new AiQuotaService(
                properties, repository, pricingService, transactionTemplate, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
        quotaService.beforeCall();
        quotaService.recordUsage("OLLAMA", "qwen2", 20L, 10L);

        var status = quotaService.status();

        Assertions.assertTrue(status.enabled());
        Assertions.assertEquals("2026-06-12", status.usageDate());
        Assertions.assertEquals(1L, status.usedCalls());
        Assertions.assertEquals(4L, status.remainingCalls());
        Assertions.assertEquals(70L, status.remainingTokens());
    }
}
